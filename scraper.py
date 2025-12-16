from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Sequence
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup


VIDEO_EXTENSIONS = {".mp4", ".mkv", ".mov", ".webm", ".ogg", ".ogv", ".m4v"}
VIDEO_HOST_HINTS = ("youtube.com", "youtu.be", "vimeo.com", "dailymotion.com")
MOTHERLESS_VIDEO_PATH = re.compile(r"^/[A-Za-z0-9]{6}$", re.IGNORECASE)


@dataclass
class VideoMatch:
    url: str
    matched_keywords: List[str]
    context: str
    source: str


@dataclass
class Config:
    url: str | None
    keywords: List[str]
    output: Path | None
    output_format: str
    timeout: int


def normalize_keywords(keywords: Iterable[str]) -> List[str]:
    return [kw.strip().lower() for kw in keywords if kw.strip()]


def ensure_url_scheme(url: str) -> str:
    """Prefix a bare domain with https:// so requests can fetch it."""

    if url.startswith(("http://", "https://", "file://")):
        return url
    return f"https://{url}"


def fetch_html(url: str, timeout: int) -> str:
    if url.startswith("file://"):
        file_path = Path(url[7:])
        return file_path.read_text(encoding="utf-8")

    headers = {"User-Agent": "video-scraper/1.0 (+https://example.com)"}
    response = requests.get(url, headers=headers, timeout=timeout)
    response.raise_for_status()
    return response.text


def build_context_text(element) -> str:
    attribute_values = []
    for key, value in element.attrs.items():
        if isinstance(value, list):
            attribute_values.extend(value)
        else:
            attribute_values.append(str(value))

    text = element.get_text(strip=True)
    pieces = attribute_values + ([text] if text else [])
    return " ".join(piece for piece in pieces if piece)


def looks_like_video_url(url: str) -> bool:
    lowered = url.lower()
    if any(host in lowered for host in VIDEO_HOST_HINTS):
        return True

    return any(lowered.endswith(ext) for ext in VIDEO_EXTENSIONS)


def collect_candidates(soup: BeautifulSoup, base_url: str) -> List[VideoMatch]:
    candidates: list[VideoMatch] = []

    # <video> tags and nested <source> tags
    for video_tag in soup.find_all("video"):
        context = build_context_text(video_tag)
        src = video_tag.get("src")
        if src:
            candidates.append(
                VideoMatch(
                    url=urljoin(base_url, src),
                    matched_keywords=[],
                    context=context,
                    source="<video> tag",
                )
            )

        for source_tag in video_tag.find_all("source"):
            source_src = source_tag.get("src")
            if source_src:
                candidates.append(
                    VideoMatch(
                        url=urljoin(base_url, source_src),
                        matched_keywords=[],
                        context=context or build_context_text(source_tag),
                        source="<source> inside <video>",
                    )
                )

    # Links that look like videos
    for link in soup.find_all("a", href=True):
        href = urljoin(base_url, link["href"])
        if looks_like_video_url(href):
            candidates.append(
                VideoMatch(
                    url=href,
                    matched_keywords=[],
                    context=build_context_text(link),
                    source="<a> link",
                )
            )

    # Open Graph video metadata
    for meta in soup.find_all("meta"):
        prop = meta.get("property") or meta.get("name")
        if prop and prop.lower() in {"og:video", "og:video:url"}:
            content = meta.get("content")
            if content:
                candidates.append(
                    VideoMatch(
                        url=urljoin(base_url, content),
                        matched_keywords=[],
                        context=prop,
                        source="<meta> tag",
                    )
                )

    return candidates


def collect_motherless_candidates(soup: BeautifulSoup, base_url: str) -> List[VideoMatch]:
    """Extract video links and IDs using Motherless-specific markup.

    Motherless video pages follow predictable 6-character alphanumeric paths
    (e.g., /AB12CD). They often appear in thumbnail grids as <a> elements or
    wrappers with data-video-id attributes. We collect those and attach nearby
    text/attribute context to improve keyword matching.
    """

    candidates: list[VideoMatch] = []
    parsed_base = urlparse(base_url)
    base_for_join = f"{parsed_base.scheme}://{parsed_base.netloc}" if parsed_base.scheme else base_url

    def nearby_text(element) -> str:
        pieces: list[str] = []

        # Immediate siblings (common structure: <a> followed by <div class="title">)
        for sibling in list(element.previous_siblings)[-2:] + list(element.next_siblings)[:2]:
            if hasattr(sibling, "get_text"):
                text = sibling.get_text(strip=True)
                if text:
                    pieces.append(text)

        # Parent container text often holds the visible title/description
        parent = element.parent
        if parent:
            parent_text = parent.get_text(strip=True)
            if parent_text:
                pieces.append(parent_text)

        return " ".join(pieces)

    for link in soup.find_all("a", href=True):
        href = link["href"]
        path = urlparse(href).path
        if not path:
            continue
        if not MOTHERLESS_VIDEO_PATH.match(path):
            continue

        url = urljoin(base_for_join, href)
        context_pieces = [build_context_text(link), nearby_text(link)]
        img = link.find("img")
        if img:
            context_pieces.append(build_context_text(img))

        context = " ".join(piece for piece in context_pieces if piece)
        candidates.append(
            VideoMatch(
                url=url,
                matched_keywords=[],
                context=context,
                source="Motherless thumbnail/link",
            )
        )

    for container in soup.find_all(attrs={"data-video-id": True}):
        video_id = str(container.get("data-video-id", "")).strip()
        if not video_id:
            continue
        path = f"/{video_id}"
        if not MOTHERLESS_VIDEO_PATH.match(path):
            continue

        url = urljoin(base_for_join, path)
        context = " ".join(
            piece
            for piece in [build_context_text(container), nearby_text(container)]
            if piece
        )

        candidates.append(
            VideoMatch(
                url=url,
                matched_keywords=[],
                context=context,
                source="Motherless data-video-id",
            )
        )

    return candidates


def summarize_context(text: str, limit: int = 140) -> str:
    condensed = re.sub(r"\s+", " ", text).strip()
    if len(condensed) <= limit:
        return condensed
    return condensed[: limit - 3].rstrip() + "..."


def filter_matches(candidates: Sequence[VideoMatch], keywords: Sequence[str]) -> List[VideoMatch]:
    keyword_set = {kw.lower() for kw in keywords}
    matches: list[VideoMatch] = []
    seen_urls: set[str] = set()

    for candidate in candidates:
        haystack = f"{candidate.url} {candidate.context}".lower()
        matched = sorted({kw for kw in keyword_set if kw in haystack})
        if not matched:
            continue

        if candidate.url in seen_urls:
            continue
        seen_urls.add(candidate.url)

        matches.append(
            VideoMatch(
                url=candidate.url,
                matched_keywords=matched,
                context=summarize_context(candidate.context),
                source=candidate.source,
            )
        )

    return matches


def render_table(matches: Sequence[VideoMatch]) -> str:
    lines = []
    for index, match in enumerate(matches, start=1):
        lines.append(f"[{index}] {match.url}")
        lines.append(f"    matches: {', '.join(match.matched_keywords)}")
        lines.append(f"    source: {match.source}")
        if match.context:
            lines.append(f"    context: {match.context}")
    return "\n".join(lines)


def save_output(output_path: Path, data: dict) -> None:
    output_path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def parse_args() -> Config:
    parser = argparse.ArgumentParser(
        description="Scrape a webpage for videos that mention specific tags or keywords.",
    )
    parser.add_argument(
        "url",
        nargs="?",
        help="Page URL to scan. Leave blank to provide interactively. Use file:// for local HTML files.",
    )
    parser.add_argument(
        "-k",
        "--keywords",
        nargs="*",
        help="Keywords or tags to look for (case-insensitive). Leave blank to provide interactively.",
    )
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        help="Optional path to write JSON results.",
    )
    parser.add_argument(
        "-f",
        "--format",
        choices=["table", "json"],
        default="table",
        help="How to print results to stdout.",
    )
    parser.add_argument(
        "-t",
        "--timeout",
        type=int,
        default=10,
        help="Request timeout in seconds when fetching remote pages.",
    )

    args = parser.parse_args()
    return Config(
        url=args.url,
        keywords=normalize_keywords(args.keywords or []),
        output=args.output,
        output_format=args.format,
        timeout=args.timeout,
    )


def prompt_for_url(current: str | None) -> str:
    prompt = "Enter the page URL or domain to scrape"
    suffix = f" [{current}]" if current else ""
    while True:
        answer = input(f"{prompt}{suffix}: ").strip()
        chosen = answer or (current or "")
        if not chosen:
            print("Please provide a URL or domain.")
            continue
        return ensure_url_scheme(chosen)


def prompt_for_keywords(current: Sequence[str]) -> List[str]:
    default_text = ", ".join(current)
    prompt = "Enter keyword(s) to search for (comma-separated for multiple)"
    suffix = f" [{default_text}]" if default_text else ""
    while True:
        answer = input(f"{prompt}{suffix}: ").strip()
        if not answer and current:
            return list(current)

        provided = re.split(r"[,\s]+", answer)
        cleaned = normalize_keywords(provided)
        if cleaned:
            return cleaned

        print("Please enter at least one keyword.")


def is_motherless_domain(url: str) -> bool:
    parsed = urlparse(url)
    host = parsed.netloc.lower()
    return host.endswith("motherless.com")


def main() -> None:
    config = parse_args()

    # Interactive prompts for convenience.
    target_url = prompt_for_url(config.url)
    keyword_list = prompt_for_keywords(config.keywords)

    html = fetch_html(target_url, timeout=config.timeout)
    soup = BeautifulSoup(html, "html.parser")

    candidates = collect_candidates(soup, base_url=target_url)
    if is_motherless_domain(target_url):
        candidates.extend(collect_motherless_candidates(soup, base_url=target_url))
    matches = filter_matches(candidates, keyword_list)

    output_data = {
        "url": target_url,
        "keywords": keyword_list,
        "matches": [
            {
                "url": match.url,
                "matched_keywords": match.matched_keywords,
                "context": match.context,
                "source": match.source,
            }
            for match in matches
        ],
    }

    if config.output_format == "json":
        print(json.dumps(output_data, indent=2))
    else:
        print(render_table(matches) if matches else "No matching videos found.")

    if config.output:
        save_output(config.output, output_data)


if __name__ == "__main__":
    main()
