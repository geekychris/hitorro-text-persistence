# Hitorro Text Persistence

This is the **hitorro-text-persistence** project, which provides text processing and indexing capabilities with database persistence integration.

## Overview

The Hitorro Text Persistence project bridges text processing functionality with database persistence, providing:

- **Text Indexing**: Integration with Lucene/Solr for full-text search
- **Phrase Extraction**: Phrase analysis and extraction from text
- **Term Matching**: Term frequency and document similarity calculations
- **ODP Page Fetching**: Web page content extraction and processing
- **Language Processing**: Integration with language models and text analysis
- **Database Integration**: Persistence of text analysis results via hitorro-basedms

## Project Structure

This is a standalone Maven JAR project.

```
hitorro-text-persistence/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/hitorro/
│   │   │       ├── basetext/
│   │   │       │   ├── commands/        # Command-line utilities
│   │   │       │   ├── indexer/         # Text indexing
│   │   │       │   ├── odppagefetcher/  # Web page fetching
│   │   │       │   ├── phrase/          # Phrase extraction
│   │   │       │   ├── termmatch/       # Term matching & similarity
│   │   │       │   └── text/            # Text processing
│   │   │       ├── language/            # Language model integration
│   │   │       └── obj/core/            # Core object utilities
│   │   └── resources/
│   └── test/
├── pom.xml
└── README.md
```

## Building

```bash
mvn clean install
```

**Note:** This project depends on several other Hitorro modules that must be built first:
- `hitorro-text-core` (text processing core)
- `hitorro-basedms` (database persistence)
- `hitorro-objretrieval` (Solr/search integration)

## Dependencies

### Hitorro Dependencies (Required)
- **hitorro-text-core 3.0.0**: Core text processing functionality
- **hitorro-basedms 3.0.0**: Database persistence layer
- **hitorro-objretrieval 3.0.0**: Solr and object retrieval

### External Dependencies
- **Apache Commons IO 2.15.1**: File and stream utilities
- **AspectJ 1.9.1**: Aspect-oriented programming support

## Key Components

### Text Indexing (`com.hitorro.basetext.indexer`)
- Lucene/Solr index creation and management
- Document indexing and retrieval
- Index optimization

### Phrase Processing (`com.hitorro.basetext.phrase`)
- Phrase extraction from text
- Phrase frequency analysis
- N-gram generation

### Term Matching (`com.hitorro.basetext.termmatch`)
- TF-IDF calculations
- Document similarity scoring
- Term frequency analysis

### ODP Page Fetcher (`com.hitorro.basetext.odppagefetcher`)
- Web page content extraction
- HTML parsing and cleaning
- Content storage

## Requirements

- Java 19 or higher
- Maven 3.6+
- Hitorro module dependencies (see above)

## Usage

This module is typically used as a library dependency in larger Hitorro applications that need text processing with database persistence.

## License

See LICENSE file for details.
