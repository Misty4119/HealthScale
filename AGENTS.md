# Working in HealthScale

HealthScale changes the HUD health scale, not the player's real maximum health. Preserve that distinction in code and documentation.

- Read `HealthScaleService`, command registration, lifecycle listeners, `PluginConfig` and bundled `config.yml` before editing scale behavior.
- Player mutations belong on the owning entity scheduler. Preserve delayed join/respawn reapplication, destination-world selection and quit/disable cleanup.
- Keep scale values finite and within configured bounds. Preserve namespaced world keys and legacy world-name lookup behavior.
- Defaults, messages, aliases and permissions must agree with the command implementation and plugin descriptor. Retain MiniMessage placeholders when editing text.
- Use the Gradle wrapper and toolchain configured by the build. Verify the distributable shaded artifact rather than assuming the plain JAR is deployable.

## Durable documentation and repository hygiene

- Read current manifests, plugin descriptors, bundled defaults and the affected source before changing public behavior claims. Keep release numbers and dependency pins in build metadata; this guide records stable rules.
- Distinguish compilation, startup smoke checks and gameplay validation. Record the exact artifact, server build, Java runtime, scenario and limitations before claiming Paper/Folia/Canvas compatibility. Descriptor flags alone do not prove runtime support.
- Keep source, sanitized fixtures, wrapper files and public documentation visible to Git. Ignore generated build/server state and explicitly local private data; never use a blanket Markdown or source-directory exclusion.
- Preserve upstream attribution, license terms and trademarks. Synchronize existing translated documentation when changing user-facing instructions.
- Use the existing focused checks for behavior changes. Documentation-only work needs link, source and diff review; report any checks not run instead of implying a build or live-server pass.
