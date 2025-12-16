# Video tag/keyword scraper

A small command-line tool that fetches a web page and reports any video URLs whose nearby text or metadata matches specific keywords or tags.

## Setup

1. Install dependencies (ideally in a virtual environment):

   ```bash
   python -m pip install -r requirements.txt
   ```

2. Run the scraper and follow the prompts (it will ask for the domain/URL and the keyword(s) to search for):

   ```bash
   python scraper.py
   ```

   You can still prefill answers via CLI options if you want:

   ```bash
   python scraper.py https://example.com -k tutorial "open source"
   ```

   Use `--format json` for machine-readable output and `--output results.json` to save the findings.

## What it looks for

- `<video>` tags and their nested `<source>` elements
- Links that appear to point to videos (common extensions or major video hosts)
- Open Graph video metadata (`og:video`)

Any candidates whose URL or nearby text contains one of the provided keywords are listed once with a brief context snippet.

## Working with local HTML

For testing, you can scan a local HTML file by prefixing the path with `file://`:

```bash
python scraper.py file:///full/path/to/sample.html -k demo "product launch"
```

## Notes

- Keywords are matched case-insensitively against the URL, tag text, and common attributes.
- Remote requests use a short timeout; adjust with `--timeout` if needed.
- The tool aims to minimize false positives, but you may still want to verify URLs before further automation.
