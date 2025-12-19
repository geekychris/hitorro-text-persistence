# Hitorro Text Persistence Extraction Notes

## Extraction Date
December 19, 2025

## What Was Done

The `hitorro-text-persistence` module was successfully extracted from the `hitorro-parent` multi-module project into its own standalone Maven project.

### New Location
- **Old**: `/Users/chris/hitorro/hitorro/hitorro-parent/hitorro-text-persistence/`
- **New**: `/Users/chris/hitorro/hitorro-text-persistence/`

### Changes Made

1. **Copied entire module** to new standalone location
2. **Updated pom.xml**:
   - Removed parent reference to `hitorro-parent`
   - Changed from child module to standalone project
   - Updated version from 2.0 to 3.0.0
   - Added all necessary Maven properties (compiler source/target, encoding)
   - Updated dependency scopes to `provided` for Hitorro modules:
     - `hitorro-text-core` (separate project)
     - `hitorro-basedms` (now standalone)
     - `hitorro-objretrieval` (separate project)
   - Updated Apache Commons IO from 2.9.0 to 2.15.1
   - Added AspectJ runtime dependency
   - Updated Maven plugin versions (compiler, surefire, jar)
3. **Created documentation**:
   - README.md
   - .gitignore
   - EXTRACTION_NOTES.md (this file)
4. **Updated parent pom**: Will remove `hitorro-text-persistence` from modules list in `hitorro-parent/pom.xml`

## Package Structure

The project contains the following main packages:

```
com.hitorro.basetext/
  ├── commands/              - Command-line utilities for text processing
  ├── indexer/               - Lucene/Solr text indexing
  ├── odppagefetcher/        - ODP (Open Directory Project) page fetching
  ├── phrase/                - Phrase extraction and analysis
  ├── termmatch/             - Term matching and document similarity
  └── text/                  - Text processing utilities
com.hitorro.language/        - Language model integration
com.hitorro.obj.core/        - Core object utilities
```

## Dependencies

### External Hitorro Modules (Required)

This project **depends on** three other Hitorro modules:

1. **hitorro-text-core** (3.0.0)
   - Provides core text processing functionality
   - Must be built/available first

2. **hitorro-basedms** (3.0.0)
   - Provides database persistence layer
   - Now a standalone project at `/Users/chris/hitorro/hitorro-basedms`

3. **hitorro-objretrieval** (3.0.0)
   - Provides Solr integration and object retrieval
   - Separate project (location TBD)

### Scope Configuration

All Hitorro dependencies are marked as `provided` scope in the POM, meaning:
- They are needed for compilation
- They are NOT bundled into this module's JAR
- They must be provided by the runtime environment
- This allows flexibility in deployment and avoids version conflicts

## Build Requirements

To successfully build this project:

1. **Build dependencies first** (in order):
   ```bash
   cd /Users/chris/hitorro/hitorro-text-core && mvn clean install
   cd /Users/chris/hitorro/hitorro-basedms && mvn clean install
   cd /Users/chris/hitorro/hitorro-objretrieval && mvn clean install
   ```

2. **Then build this project**:
   ```bash
   cd /Users/chris/hitorro/hitorro-text-persistence && mvn clean install
   ```

## Known Issues

### Dependency Chain

This module is part of a dependency chain:
- `hitorro-text-core` → `hitorro-text-persistence` → Applications

Some dependencies may have their own compilation issues:
- `hitorro-basedms` has missing dependencies (hitorro-util, hitorro-base)
- `hitorro-objretrieval` may need extraction/creation
- `hitorro-text-core` may need extraction/creation

### AspectJ Configuration

The module uses AspectJ for AOP support:
- Requires aspectjweaver agent during testing
- Maven Surefire plugin configured with javaagent argument
- May need adjustment based on local Maven repository path

## Features Provided

This module bridges text processing with database persistence:

1. **Text Indexing**: Creates and manages Lucene/Solr indexes
2. **Phrase Analysis**: Extracts and analyzes phrases from documents
3. **Term Matching**: Calculates TF-IDF and document similarity
4. **Web Scraping**: Fetches and processes web pages
5. **Persistence Integration**: Stores results in database via hitorro-basedms

## Next Steps

1. Extract/create missing dependency modules:
   - `hitorro-text-core`
   - `hitorro-objretrieval`
2. Resolve compilation issues in `hitorro-basedms`
3. Update parent pom to remove this module
4. Test build chain
5. Verify functionality with integration tests

## Build Status

Current build status: **UNKNOWN** (not yet tested)

Expected issues:
- Missing dependency modules
- Transitive dependency problems from hitorro-basedms

To attempt a build:
```bash
cd /Users/chris/hitorro/hitorro-text-persistence
mvn clean compile
```
