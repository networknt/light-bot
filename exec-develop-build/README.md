# YAML configuration migration

Build configuration loading and merged configuration output use SnakeYAML Engine
3.0.1 with the YAML 1.2 Core schema. Readers and writers are local to each operation.
Public configuration APIs, `.yml` / `.yaml` / `.json` fallback order, and duplicate-key
last-value-wins behavior are unchanged. Invalid YAML fails with a file-naming error
and preserves the Engine exception; it does not fall through to a different file.
If no configuration exists at any supported location, loading still returns null.

The default configuration utility emits a startup warning about scalar changes.
Review deployed and repository configuration files before rollout; that warning
does not validate their compatibility.

| Input | Classic SnakeYAML | Engine Core |
| --- | --- | --- |
| `on`, `off`, `yes`, `no` | Boolean | String |
| `0755` | Integer 493 | Integer 755 |
| `0o755` | String | Integer 493 |
| `0b11` | Integer 3 | String |
| `1_000` | Integer 1000 | String |
| `1:30` | Integer 90 | String |
| `1:30.5` | Float 90.5 | String |
| `1_0.5` | Float 10.5 | String |
| `2026-10-24` | Date | String |

Use `true`/`false`, decimal or YAML 1.2 octal numbers, and quote values that must
remain strings. Legacy explicit scalar tags (such as `!!timestamp`), Java object
tags, and YAML 1.1 merge keys are not a compatibility contract of the new loader.
Engine's resource limits also apply.

Merged output uses block-style UTF-8 YAML. Dates, calendars, and temporal values
are emitted as ISO strings. Serialization finishes before opening the output file,
so unsupported values do not truncate an existing file. Serialization errors are
reported as IOExceptions; output I/O failures can still leave a partial file.

Engine is a direct dependency of this module. The parent retains the classic
SnakeYAML 2.7 management pin for third-party transitive consumers. Existing light-4j
dependencies still supply classic SnakeYAML; this change does not migrate those
libraries or remove their compatibility exports.

Project version 2.3.4 and light-4j version 2.3.3 are unchanged. Scalar behavior
changes require coordinated release review before deployment.
