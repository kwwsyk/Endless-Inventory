# NeoForge dedicated-server startup (#48)

## Evidence and scope

Issue: https://github.com/kwwsyk/Endless-Inventory/issues/48
Reporter log: https://github.com/kwwsyk/Endless-Inventory/issues/48#issuecomment-6213739094

The log specifies Minecraft 1.21.1, NeoForge 21.1.262, Java 21 and
Endless Inventory-neoforge-1.21.1-1.1.4.jar. Its causal chain is:

```text
ModInitializer.<init> (line 58)
  -> ClientConfig.<clinit> / ClientConfig.<init>
  -> ClientConfigs.<clinit> (line 31)
  -> AttachedMenuConfig.<clinit> / AttachedMenuConfig.<init>
  -> AttachedMenuScreenLayout.getDefault (line 233)
  -> RuntimeException: Attempted to load class
     net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER
```

This is a client-only class loading failure during mod construction, not an
attempt to render a frame on the server. Passing CLIENT to registerConfig does
not prevent Java from evaluating ClientConfig.CONFIG_SPEC first. Default
branch 1.21.1 at 7620afa still does this unconditionally. The earlier #43 fix
changed SpecifiedMenuAttachingConfig and does not remove this startup path.

Move CLIENT configuration registration and the config-screen extension into
ClientModInitializer.init, called only under FMLEnvironment.dist.isClient().
SERVER and STARTUP configuration registration remain on both physical sides.
This patch addresses the logged failure; dedicated-server startup and subsequent
player actions still need runtime verification.

## Build

Use JDK 21 and the checked-in wrapper. On Windows use gradlew.bat.

```sh
./gradlew test :neoforge:build --no-daemon
./gradlew test :neoforge:build -Pneoforge_version=21.1.262 --no-daemon
```

Run with the repository's pinned NeoForge 21.1.211 and the reporter's 21.1.262.
The version property controls the dev/runtime dependency; it does not itself
prove the packaged mod works on a production server.

## Dedicated-server acceptance checks

1. Prepare a separate Minecraft 1.21.1 NeoForge dedicated-server test directory,
   using Java 21, with only the built NeoForge mod JAR and required dependencies.
   Accept Minecraft's EULA before starting it. Do not substitute an integrated
   singleplayer server: the physical process must be DEDICATED_SERVER.
2. Launch the installed server with its normal run script and nogui. Expected:
   startup reaches `Done (...)! For help, type "help"`; the console accepts
   commands; there is no ExceptionInInitializerError / invalid-dist Screen
   loading error from Endless Inventory. Stop normally and start again.
3. Verify SERVER and STARTUP configs are registered. On a fresh server, EI's
   client config should not be generated as a consequence of mod construction.
   Do not delete existing server files merely to satisfy this check.
4. Join using a matching client with the same patched mod. Open E and I, store
   and retrieve an item, disconnect/rejoin and verify contents persist. Repeat
   after a server restart, then with the reporter's full modpack on a copy of
   its world. This checks paths not reached by a startup-only smoke test.
5. Launch the client normally: verify the client config loads/saves, the mod-list
   config screen opens, key bindings work, and singleplayer E/I still work.
   A physical-client guard must also allow integrated singleplayer clients.

For a developer runtime (after accepting the EULA in its run directory):

```sh
./gradlew :neoforge:runServer -Pneoforge_version=21.1.262 --no-daemon
./gradlew :neoforge:runClient -Pneoforge_version=21.1.262 --no-daemon
```

Record the built commit, JDK/loader versions, startup log through Done and
client join/restart outcomes. Do not report runtime success from source review.
