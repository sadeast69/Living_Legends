package com.worldremembers.livinglegends.config;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;

public final class LivingLegendsConfigManager {
    public static final Path CONFIG_ROOT_RELATIVE = Path.of("config", "world_remembers", "living_legends");
    public static final String COMMON_CONFIG_FILE_NAME = "common.json";
    public static final String COMMENTED_CONFIG_FILE_NAME = "common.json5";
    public static final String COMMENTED_JSONC_CONFIG_FILE_NAME = "common.jsonc";
    public static final String NAME_PACKS_FOLDER_NAME = "name_packs";
    public static final String EXPORTS_FOLDER_NAME = "exports";
    public static final String DEFAULT_NAME_PACK_FILE_NAME = "default.json";

    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private static volatile LivingLegendsConfig current = LivingLegendsConfig.defaults();
    private static volatile Path currentConfigPath = CONFIG_ROOT_RELATIVE.resolve(COMMENTED_CONFIG_FILE_NAME);

    private LivingLegendsConfigManager() {
    }

    public static LivingLegendsConfig current() {
        return current;
    }

    public static Path currentConfigPath() {
        return currentConfigPath;
    }

    public static LivingLegendsConfigLoadResult loadOnServerStart(
            Path gameRoot,
            Consumer<String> infoLogger,
            Consumer<String> warningLogger
    ) {
        return loadOrCreate(gameRoot, "server start", infoLogger, warningLogger);
    }

    public static LivingLegendsConfigLoadResult reload(
            Path gameRoot,
            Consumer<String> infoLogger,
            Consumer<String> warningLogger
    ) {
        return loadOrCreate(gameRoot, "reload", infoLogger, warningLogger);
    }

    public static Path configRoot(Path gameRoot) {
        return requireGameRoot(gameRoot).resolve(CONFIG_ROOT_RELATIVE);
    }

    public static Path commonConfigPath(Path gameRoot) {
        return configRoot(gameRoot).resolve(COMMENTED_CONFIG_FILE_NAME);
    }

    public static Path namePacksPath(Path gameRoot) {
        return configRoot(gameRoot).resolve(NAME_PACKS_FOLDER_NAME);
    }

    public static Path exportsPath(Path gameRoot) {
        return configRoot(gameRoot).resolve(EXPORTS_FOLDER_NAME);
    }

    private static synchronized LivingLegendsConfigLoadResult loadOrCreate(
            Path gameRoot,
            String reason,
            Consumer<String> infoLogger,
            Consumer<String> warningLogger
    ) {
        Path root = configRoot(gameRoot);
        Path json5Path = root.resolve(COMMENTED_CONFIG_FILE_NAME);
        Path jsoncPath = root.resolve(COMMENTED_JSONC_CONFIG_FILE_NAME);
        Path legacyJsonPath = root.resolve(COMMON_CONFIG_FILE_NAME);
        Path configPath = selectConfigPath(json5Path, jsoncPath, legacyJsonPath);
        Path namePacksPath = root.resolve(NAME_PACKS_FOLDER_NAME);
        Path exportsPath = root.resolve(EXPORTS_FOLDER_NAME);

        try {
            createFolders(root, namePacksPath, exportsPath);

            if (Files.notExists(configPath)) {
                LivingLegendsConfig defaults = LivingLegendsConfig.defaults();
                writeConfig(json5Path, defaults);
                current = defaults;
                currentConfigPath = json5Path;
                String message = "Created default " + WorldRemembersLivingLegends.MOD_NAME + " commented config at " + json5Path;
                info(infoLogger, message);
                return new LivingLegendsConfigLoadResult(
                        defaults,
                        json5Path,
                        namePacksPath,
                        exportsPath,
                        true,
                        true,
                        false,
                        null,
                        message
                );
            }

            LivingLegendsConfig loaded = readConfig(configPath);
            if (configPath.equals(legacyJsonPath) && Files.notExists(json5Path) && Files.notExists(jsoncPath)) {
                Path backupPath = legacyJsonPath.resolveSibling(COMMON_CONFIG_FILE_NAME + ".backup_before_json5_migration");
                Files.copy(legacyJsonPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                writeConfig(json5Path, loaded);
                configPath = json5Path;
                info(infoLogger, "Migrated " + WorldRemembersLivingLegends.MOD_NAME
                        + " config from common.json to commented common.json5. Backup: " + backupPath);
            } else if (configPath.equals(json5Path) || configPath.equals(jsoncPath)) {
                writeConfig(configPath, loaded);
            }
            current = loaded;
            currentConfigPath = configPath;
            LivingLegendsConfig.ValidationSummary summary = loaded.validationSummary();
            String summaryMessage = "World Remembers config validation: " + summary.compact();
            if (summary.warnings() > 0) {
                warn(warningLogger, summaryMessage);
            } else {
                info(infoLogger, summaryMessage);
            }
            for (String warning : loaded.validationWarnings()) {
                warn(warningLogger, "Config warning: " + warning);
            }

            if (loaded.debug.enabled && loaded.debug.logConfigLoad) {
                info(infoLogger, "Loaded " + WorldRemembersLivingLegends.MOD_NAME + " config for " + reason + " from " + configPath);
            }

            return new LivingLegendsConfigLoadResult(
                    loaded,
                    configPath,
                    namePacksPath,
                    exportsPath,
                    false,
                    false,
                    false,
                    null,
                    "Loaded config"
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return recoverFromMalformedConfig(configPath, namePacksPath, exportsPath, exception, warningLogger);
        } catch (IOException exception) {
            LivingLegendsConfig defaults = LivingLegendsConfig.defaults();
            current = defaults;
            currentConfigPath = configPath;
            String message = "Could not load " + WorldRemembersLivingLegends.MOD_NAME
                    + " config at " + configPath + "; using defaults. Cause: " + exception.getMessage();
            warn(warningLogger, message);
            return new LivingLegendsConfigLoadResult(
                    defaults,
                    configPath,
                    namePacksPath,
                    exportsPath,
                    false,
                    true,
                    false,
                    null,
                    message
            );
        }
    }

    private static LivingLegendsConfigLoadResult recoverFromMalformedConfig(
            Path configPath,
            Path namePacksPath,
            Path exportsPath,
            RuntimeException exception,
            Consumer<String> warningLogger
    ) {
        LivingLegendsConfig defaults = LivingLegendsConfig.defaults();
        current = defaults;
        currentConfigPath = configPath;

        Path backupPath = null;
        try {
            backupPath = backupMalformedConfig(configPath);
            writeConfig(configPath, defaults);
        } catch (IOException backupException) {
            warn(warningLogger, "Could not back up malformed " + WorldRemembersLivingLegends.MOD_NAME
                    + " config at " + configPath + ". Cause: " + backupException.getMessage());
        }

        String backupMessage = backupPath == null ? "No backup was created." : "Backup: " + backupPath;
        String message = "Malformed " + WorldRemembersLivingLegends.MOD_NAME + " config at " + configPath
                + "; using defaults. " + backupMessage + " Cause: " + exception.getMessage();
        warn(warningLogger, message);

        return new LivingLegendsConfigLoadResult(
                defaults,
                configPath,
                namePacksPath,
                exportsPath,
                false,
                true,
                true,
                backupPath,
                message
        );
    }

    private static void createFolders(Path root, Path namePacksPath, Path exportsPath) throws IOException {
        Files.createDirectories(root);
        Files.createDirectories(namePacksPath);
        Files.createDirectories(exportsPath);
        createDefaultNamePack(namePacksPath);
    }

    private static void createDefaultNamePack(Path namePacksPath) throws IOException {
        Path defaultPackPath = namePacksPath.resolve(DEFAULT_NAME_PACK_FILE_NAME);
        if (Files.exists(defaultPackPath)) {
            return;
        }

        Files.writeString(defaultPackPath, defaultNamePackJson());
    }

    private static String defaultNamePackJson() {
        return """
                {
                  "packId": "living_legends_default",
                  "version": 2,
                  "styleId": "vanilla_adventure",
                  "notes": "Stage 8 name packs are prepared for future overrides. Built-in patterns and tokens are active now.",
                  "patterns": [],
                  "tokens": []
                }
                """;
    }

    private static LivingLegendsConfig readConfig(Path configPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            String fileName = configPath.getFileName().toString().toLowerCase();
            return LivingLegendsConfig.fromMap(fileName.endsWith(".json5") || fileName.endsWith(".jsonc")
                    ? SimpleJson.parseObjectWithComments(reader)
                    : SimpleJson.parseObject(reader));
        }
    }

    private static void writeConfig(Path configPath, LivingLegendsConfig config) throws IOException {
        Files.createDirectories(configPath.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            String fileName = configPath.getFileName().toString().toLowerCase();
            writer.write(fileName.endsWith(".json5") || fileName.endsWith(".jsonc")
                    ? ConfigCommentWriter.write(config)
                    : SimpleJson.stringify(config.toMap()));
            writer.newLine();
        }
    }

    private static Path selectConfigPath(Path json5Path, Path jsoncPath, Path legacyJsonPath) {
        if (Files.exists(json5Path)) {
            return json5Path;
        }
        if (Files.exists(jsoncPath)) {
            return jsoncPath;
        }
        if (Files.exists(legacyJsonPath)) {
            return legacyJsonPath;
        }
        return json5Path;
    }

    private static Path backupMalformedConfig(Path configPath) throws IOException {
        Path backupPath = configPath.resolveSibling(
                configPath.getFileName() + ".malformed-" + BACKUP_TIMESTAMP.format(Instant.now()) + ".bak"
        );
        Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }

    private static Path requireGameRoot(Path gameRoot) {
        return Objects.requireNonNull(gameRoot, "gameRoot");
    }

    private static void info(Consumer<String> logger, String message) {
        if (logger != null) {
            logger.accept(message);
        }
    }

    private static void warn(Consumer<String> logger, String message) {
        if (logger != null) {
            logger.accept(message);
        }
    }
}
