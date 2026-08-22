# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Jackson XML dataformat module extends Jackson to serialize/deserialize XML using the STAX (Streaming API for XML) processing engine. The goal is to emulate JAXB "code-first" data binding while leveraging Jackson's rich type system and features. This is NOT a full JAXB clone or general-purpose XML toolkit.

**Key Principle:** Any XML written by this module MUST be readable by this module (guaranteed round-trip support).

## Build & Test Commands

This is a Maven-based project. Use the Maven wrapper (`./mvnw`) for consistent builds.

### Basic Commands

```bash
# Clean and build
./mvnw clean install

# Run all tests
./mvnw test

# Run tests without building
./mvnw surefire:test

# Run a single test class
./mvnw test -Dtest=XmlMapperTest

# Run a single test method
./mvnw test -Dtest=XmlMapperTest#testSimpleSerialization

# Build without running tests
./mvnw clean install -DskipTests

# Generate code coverage report (JaCoCo)
./mvnw clean test
# Report available at: target/site/jacoco/index.html
```

### JDK Baseline & JDK-Specific Testing

- Main sources compile to Java 8 bytecode (`<release>8</release>` on JDK 9+).
- JDK 17+ specific tests live in `src/test-jdk17/java/` and are added as an extra
  test source root by a Maven profile activated on `[17,)`. These cover Java Records
  support and other modern language features; they compile/run with `release 17`.

### Working with Test Files

- Main test sources: `src/test/java/com/fasterxml/jackson/dataformat/xml/`
- JDK 17+ tests: `src/test-jdk17/java/com/fasterxml/jackson/dataformat/xml/`
- **Test base class: `XmlTestUtil`** — extend this for new tests (JUnit 5 based;
  provides `newMapper()`, shared POJOs, and assertion helpers). All ~150 test classes
  now use it.
- `XmlTestBase` is deprecated (since 2.19), JUnit 3/4 `TestCase`-based, and no longer
  extended by any test. Do not use it for new tests.
- Tests use JUnit 5 (`org.junit.jupiter.api.Test`, static `Assertions` imports).
  JUnit 4 remains on the test classpath only as a legacy dependency.

## Repository Branch Structure

- `2.x` — active development branch for the next 2.x minor (currently `2.23.0-SNAPSHOT`)
- `2.22` — maintenance branch for the current 2.22.x patch releases (merged up into `2.x`)
- `master` — 3.x development (currently `3.0.0-rc3-SNAPSHOT`)
- When creating PRs against 2.x work, target `2.x` (or the relevant maintenance branch
  if the fix must ship in a patch release).

Version numbers are inherited from the `com.fasterxml.jackson:jackson-base` parent POM.

## High-Level Architecture

### Core Components Flow

```
User Code
    ↓
XmlMapper (extends ObjectMapper) ← Primary API entry point
    ↓
XmlFactory (extends JsonFactory) ← Creates parsers/generators
    ├→ FromXmlParser (XML → JSON token stream)
    │    ├─ XmlTokenStream (STAX abstraction layer)
    │    └─ XMLStreamReader (Woodstox)
    │
    └→ ToXmlGenerator (JSON token stream → XML)
         └─ XMLStreamWriter (Woodstox)
```

### Builder-Style Construction

Both mapper and factory support the 2.x builder API (preferred over legacy constructors):

```java
XmlMapper mapper = XmlMapper.builder()
    .enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
    .defaultUseWrapper(false)
    .build();
```

`XmlMapper.Builder` extends `MapperBuilder`; `XmlFactoryBuilder` configures the
`XMLInputFactory`/`XMLOutputFactory`, the `XmlNameProcessor`, and parser/generator
format features.

### Module Registration System

`JacksonXmlModule` (a `SimpleModule`) centralizes XML-specific configuration:
- Registers `XmlBeanSerializerModifier` - hooks into serializer creation
- Registers `XmlBeanDeserializerModifier` - hooks into deserializer creation
- Adds `JacksonXmlAnnotationIntrospector` - processes @JacksonXml* annotations
- Configures text element naming conventions

Module is automatically registered when creating an `XmlMapper`.

### Serialization Pipeline (Object → XML)

```
Object
  → XmlMapper.writeValue()
  → ToXmlGenerator (implements JsonGenerator)
  → XmlBeanSerializerModifier (customizes bean serializers)
  → XmlBeanSerializer + XmlBeanPropertyWriter
     ├─ Determines element vs attribute
     ├─ Handles namespace mappings
     └─ Manages wrapper elements for collections
  → XmlSerializerProvider (handles root element naming)
  → XMLStreamWriter (Woodstox STAX)
  → XML Output
```

**Key Classes:**
- `XmlBeanSerializerBase` / `XmlBeanSerializer` - Extend Jackson's `BeanSerializer` with XML-specific logic
- `XmlBeanPropertyWriter` - Determines if property is element or attribute
- `UnwrappingXmlBeanSerializer` - Handles `@JsonUnwrapped` properties
- `XmlRootNameLookup` - Caches root element name calculations
- `DefaultXmlPrettyPrinter` (implements `XmlPrettyPrinter`) - XML-aware formatting

### Deserialization Pipeline (XML → Object)

```
XML Input
  → XMLStreamReader (STAX/Woodstox)
  → FromXmlParser (implements JsonParser)
  → XmlTokenStream (converts XML events to JSON tokens)
     ├─ START_ELEMENT → START_OBJECT
     ├─ Attributes → FIELD_NAME + VALUE pairs
     ├─ Text content → VALUE_STRING (field name = "")
     └─ END_ELEMENT → END_OBJECT
  → XmlBeanDeserializerModifier (customizes deserializers)
     ├─ Renames wrapper properties
     ├─ Handles @JacksonXmlText
     └─ Manages collection wrapping/unwrapping
  → Standard Jackson Deserializers
  → Object
```

**Key Classes:**
- `FromXmlParser` - Wraps STAX reader, exposes JSON-like token stream
- `XmlTokenStream` - Intermediate abstraction with token replay/lookahead
- `WrapperHandlingDeserializer` - Handles wrapped/unwrapped collections
- `ElementWrapper` / `ElementWrappable` - Wrapper-name metadata plumbing
- `XmlReadContext` - Tracks parsing context (current element, namespace)
- `XmlDeserializationContext` - XML-specific `DeserializationContext` subclass
- `XmlTextDeserializer` - Deserializes element text content

### Collection Wrapping Pattern

A critical XML-specific concern is how collections are represented:

**Wrapped (default behavior):**
```xml
<items>
  <item>A</item>
  <item>B</item>
</items>
```

**Unwrapped:**
```xml
<item>A</item>
<item>B</item>
```

Control via:
- `@JacksonXmlElementWrapper(useWrapping = false)` per property
- `XmlMapper.builder().defaultUseWrapper(false)` (or `JacksonXmlModule.setDefaultUseWrapper(false)`) globally
- `WrapperHandlingDeserializer` implements the complex logic

### Annotation System

**Jackson XML Annotations** (`com.fasterxml.jackson.dataformat.xml.annotation`):
- `@JacksonXmlRootElement` - Set root element name/namespace
- `@JacksonXmlProperty(isAttribute=true)` - Mark property as XML attribute
- `@JacksonXmlElementWrapper` - Control collection wrapper elements
- `@JacksonXmlText` - Property represents element text content (not a child element)
- `@JacksonXmlCData` - Wrap value in CDATA section

**JAXB Support:**
Uses optional `jackson-module-jakarta-xmlbind-annotations` dependency. The `XmlJaxbAnnotationIntrospector` can process JAXB annotations for interoperability.

**Polymorphic Type Handling:**
`XmlTypeResolverBuilder` and `DefaultingXmlTypeResolverBuilder` adapt Jackson's type
resolution (`@JsonTypeInfo`, default typing) to XML — notably making `As.PROPERTY`
inclusion work as an XML attribute.

### STAX Integration

**Woodstox** is the preferred STAX implementation (faster and more reliable than JDK's default).

**Factory Configuration:**
- `XmlFactory` manages `XMLInputFactory` and `XMLOutputFactory`
- Security defaults: external entities disabled, DTD processing disabled
- `IS_REPAIRING_NAMESPACES = true` for automatic namespace handling
- `IS_COALESCING = true` to simplify text content processing
- `Stax2JacksonReaderAdapter` bridges non-Woodstox (Stax2-less) readers

**Custom STAX Configuration Example:**
```java
XMLInputFactory ifactory = new WstxInputFactory();
ifactory.setProperty(WstxInputProperties.P_MAX_ATTRIBUTE_SIZE, 32000);
XmlFactory xf = XmlFactory.builder()
    .xmlInputFactory(ifactory)
    .build();
XmlMapper mapper = new XmlMapper(xf);
```

### Name Processing

`XmlNameProcessor` allows custom XML name transformations, configured via
`XmlFactoryBuilder.nameProcessor(...)` and applied during both serialization and
deserialization. `XmlNameProcessors` provides ready-made implementations:
`newPassthroughProcessor()` (default), `newReplacementProcessor()`,
`newBase64Processor()`, and `newAlwaysOnBase64Processor()` — the latter two encode
names that are not valid XML names.

### Feature Flags

**FromXmlParser.Feature** (deserialization) — the complete set:
- `AUTO_DETECT_XSI_TYPE` (default off) - Process `xsi:type` attributes for polymorphism
- `EMPTY_ELEMENT_AS_NULL` (default off) - Treat `<element/>` as null
- `PROCESS_XSI_NIL` (default **on**) - Honor `xsi:nil="true"`

(Note: `ENFORCE_VALID_ROOT_NAME` appears in the source but is commented out — it is
not an available feature.)

**ToXmlGenerator.Feature** (serialization) — the complete set:
- `WRITE_XML_DECLARATION` - Output `<?xml version="1.0"?>`
- `WRITE_STANDALONE_YES_TO_XML_DECLARATION` - Add `standalone="yes"` (needs the above)
- `WRITE_XML_1_1` - Use XML 1.1
- `WRITE_NULLS_AS_XSI_NIL` - Add `xsi:nil="true"` for null values
- `UNWRAP_ROOT_OBJECT_NODE` - For a single-entry root `ObjectNode`, use its key as
  root element name (will default to enabled in 3.0)
- `AUTO_DETECT_XSI_TYPE` - Add `xsi:type` for polymorphic types
- `WRITE_XML_SCHEMA_CONFORMING_FLOATS` - Emit `INF`/`-INF`/`NaN` per XML Schema
  (will default to enabled in 3.0)

All default to disabled except `PROCESS_XSI_NIL` on the parser side.
Configure via `XmlMapper.builder().enable(...)/.disable(...)`, or
`XmlMapper.enable(feature)` / `XmlMapper.disable(feature)`.

## Source Code Structure

```
src/main/java/com/fasterxml/jackson/dataformat/xml/
├── XmlMapper.java              - Primary API, extends ObjectMapper (has Builder)
├── XmlFactory.java             - Creates parsers/generators
├── XmlFactoryBuilder.java      - Builder for XmlFactory (STAX factories, name processor)
├── JacksonXmlModule.java       - Module for XML configuration
├── JacksonXmlAnnotationIntrospector.java / XmlAnnotationIntrospector.java
├── XmlTypeResolverBuilder.java / DefaultingXmlTypeResolverBuilder.java
├── XmlNameProcessor.java / XmlNameProcessors.java  - XML name transformation
├── XmlPrettyPrinter.java
├── annotation/                 - @JacksonXml* annotations
├── deser/                      - Deserialization (XML → Object)
│   ├── FromXmlParser.java      - Parser implementation
│   ├── XmlTokenStream.java     - STAX abstraction layer
│   ├── XmlBeanDeserializerModifier.java
│   ├── WrapperHandlingDeserializer.java
│   ├── ElementWrapper.java / ElementWrappable.java
│   ├── XmlDeserializationContext.java / XmlReadContext.java
│   └── XmlTextDeserializer.java
├── ser/                        - Serialization (Object → XML)
│   ├── ToXmlGenerator.java     - Generator implementation
│   ├── XmlBeanSerializer.java / XmlBeanSerializerBase.java
│   ├── UnwrappingXmlBeanSerializer.java
│   ├── XmlBeanPropertyWriter.java
│   ├── XmlBeanSerializerModifier.java
│   └── XmlSerializerProvider.java
├── jaxb/                       - JAXB integration
│   └── XmlJaxbAnnotationIntrospector.java
└── util/                       - Utilities
    ├── StaxUtil.java                  - STAX exception handling
    ├── XmlRootNameLookup.java         - Root element naming
    ├── DefaultXmlPrettyPrinter.java
    ├── Stax2JacksonReaderAdapter.java
    ├── AnnotationUtil.java / TypeUtil.java / XmlInfo.java
    └── CaseInsensitiveNameSet.java

src/test/java/.../xml/
├── deser/                      - Deserialization tests (+ builder/, convert/, creator/)
├── ser/                        - Serialization tests (+ dos/)
├── stream/                     - Low-level parser/generator tests (+ dos/)
├── lists/                      - Collection handling tests
├── node/                       - Tree model (JsonNode) tests
├── misc/                       - Assorted feature tests
├── adapters/, incr/, interop/, jaxb/, vld/, woodstox/
├── fuzz/                       - Regression tests from OSS-Fuzz findings
├── tofix/                      - Tests for known issues (see below)
├── testutil/                   - Test helpers (+ failure/ annotations)
├── XmlTestUtil.java            - Base test class for new tests
└── XmlTestBase.java            - Deprecated, unused legacy base class

src/test-jdk17/java/.../xml/
├── jdk17/                      - JDK 17+ feature tests
└── records/                    - Java Records support tests (+ tofix/)
```

## Test Organization

- Tests are organized by feature area (deser, ser, lists, stream, node, etc.)
- Issue-specific tests named like `RootName374Test.java` (GitHub issue #374)
- `tofix/` holds tests for known limitations/bugs. These extend `XmlTestUtil` and are
  annotated `@JacksonTestFailureExpected` (see `testutil/failure/`), which inverts the
  result: the test *fails* the build if it unexpectedly starts passing. When you fix a
  bug, remove the annotation and move the test into the proper package.
- `fuzz/` holds regression tests for OSS-Fuzz reports; `*/dos/` holds
  denial-of-service / resource-limit tests.
- JDK 17+ tests in a separate source directory, auto-enabled by Maven profile.

## Known Limitations

- **Tree Model** (`JsonNode`): Does not perfectly match XML infoset
  - Mixed content (text + elements) not fully supported
  - Repeated elements handled specially (see #403)
- **Root Values**: Work best with POJOs; primitives, Strings, Collections have limitations
- **Collection Wrapping**: Default behavior differs between Jackson and JAXB annotations
- **Namespace URIs**: Not verified during deserialization (only local names matched)
- **Polymorphic Types**: Some inclusion mechanisms unsupported (e.g., `WRAPPER_ARRAY`)

See README.md "Known Limitations" section for complete list.

## Development Notes

### Release Notes
Every user-visible change gets an entry in `release-notes/VERSION-2.x` (issue number,
one-line description, credit line) and, for external contributors, `release-notes/CREDITS-2.x`.

### Security
- External entity expansion disabled by default (XXE prevention)
- DTD processing disabled
- These are configured in `XmlFactory` constructor

### Streaming vs Databinding
- Low-level streaming (direct `FromXmlParser`/`ToXmlGenerator` use) is possible but not primary use case
- Databinding layer includes necessary XML-specific workarounds
- For incremental/partial reading/writing, combine STAX APIs with `XmlMapper` (see `incr/` tests)

### Modifying Serializers/Deserializers
Use the Modifier pattern:
- Extend `XmlBeanSerializerModifier` for serialization customization
- Extend `XmlBeanDeserializerModifier` for deserialization customization
- Register via `JacksonXmlModule` or custom module

### Adding New Features
1. Consider if it's a parser/generator feature (token stream level) or serializer/deserializer feature (databinding level)
2. Parser/Generator: Modify `FromXmlParser`/`ToXmlGenerator` and potentially `XmlTokenStream`
3. Databinding: Use Modifier pattern or custom `JsonSerializer`/`JsonDeserializer`
4. Add feature flag if it's optional behavior (defaults must stay backwards-compatible in 2.x)
5. Add tests in appropriate subdirectory, extending `XmlTestUtil`
6. Add a release-notes entry

## Dependencies

**Core (compile):**
- `jackson-core`, `jackson-databind`, `jackson-annotations` (from parent BOM)
- `stax2-api` (enhanced STAX API)
- `woodstox-core` (STAX implementation)
- `stax-api` (`provided` scope only, for old JDK compatibility)

**Test:**
- JUnit 5 (`junit-jupiter`, `junit-jupiter-api`) — used by all tests
- JUnit 4 (`junit`) — legacy, only for the deprecated `XmlTestBase`
- `jackson-module-jakarta-xmlbind-annotations` + `jakarta.xml.bind-api` (JAXB interop testing)
- `sjsxp` (alternative STAX impl for testing)

**Version Management:**
Versions inherited from `com.fasterxml.jackson:jackson-base` parent POM, which manages the Jackson BOM (bill of materials).
