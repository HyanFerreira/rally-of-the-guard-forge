package net.hfstack.rallyguard.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.hfstack.rallyguard.RallyOfTheGuard;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Disables GuardVillagers' Hero of the Village requirement for following.
 */
public final class GuardVillagersConfigPatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RallyOfTheGuard.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FORGE_FOLLOW_HERO_KEY =
            "Have guards only follow the player if they have hero of the village?";
    private static boolean runtimePatchLogged;

    private GuardVillagersConfigPatcher() {
    }

    public static void patchFollowHeroConfig() {
        try {
            Path cfgDir = FMLPaths.CONFIGDIR.get();
            Path json = cfgDir.resolve("guardvillagers.json");
            Path toml = cfgDir.resolve("guardvillagers-common.toml");

            if (Files.exists(json)) {
                patchJson(json);
            } else if (Files.exists(toml)) {
                patchToml(toml);
            } else {
                LOGGER.info("[{}] Config do GuardVillagers ainda nao encontrado; aplicando patch em runtime.",
                        RallyOfTheGuard.MOD_ID);
            }

            forceRuntimeFollowHeroConfigValue();
        } catch (Exception e) {
            LOGGER.error("[{}] Falha ao ajustar followHero no GuardVillagers", RallyOfTheGuard.MOD_ID, e);
        }
    }

    public static void forceRuntimeFollowHeroConfigValue() {
        try {
            Class<?> guardConfig = Class.forName("tallestegg.guardvillagers.configuration.GuardConfig");
            Object commonConfig = guardConfig.getField("COMMON").get(null);
            Field followHeroValueField = commonConfig.getClass().getField("followHero");
            Object followHeroValue = followHeroValueField.get(commonConfig);
            Method set = followHeroValue.getClass().getMethod("set", Object.class);
            set.invoke(followHeroValue, Boolean.FALSE);

            forceRuntimeFollowHeroFlag();
            if (!runtimePatchLogged) {
                runtimePatchLogged = true;
                LOGGER.info("[{}] GuardVillagers runtime ajustado: followHero=false", RallyOfTheGuard.MOD_ID);
            }
        } catch (ClassNotFoundException ignored) {
            LOGGER.warn("[{}] GuardVillagers nao esta disponivel para ajustar followHero em runtime.",
                    RallyOfTheGuard.MOD_ID);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[{}] Falha ao ajustar followHero em runtime no GuardVillagers", RallyOfTheGuard.MOD_ID, e);
        }
    }

    public static void forceRuntimeFollowHeroFlag() {
        try {
            Class<?> guardConfig = Class.forName("tallestegg.guardvillagers.configuration.GuardConfig");
            guardConfig.getField("followHero").setBoolean(null, false);
        } catch (ClassNotFoundException ignored) {
            // GuardVillagers is not loaded yet.
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[{}] Falha ao ajustar campo followHero no GuardVillagers", RallyOfTheGuard.MOD_ID, e);
        }
    }

    private static void patchJson(Path file) throws IOException {
        JsonObject root;
        try (Reader r = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        }

        boolean needsWrite = !root.has("followHero") || root.get("followHero").getAsBoolean();
        if (needsWrite) {
            root.addProperty("followHero", false);
            try (Writer w = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            LOGGER.info("[{}] GuardVillagers JSON ajustado: followHero=false", RallyOfTheGuard.MOD_ID);
        } else {
            LOGGER.info("[{}] GuardVillagers JSON ja esta com followHero=false", RallyOfTheGuard.MOD_ID);
        }
    }

    private static void patchToml(Path file) throws IOException {
        String original = Files.readString(file, StandardCharsets.UTF_8);
        String content = removeStaleFabricKey(original);

        Pattern followHeroPattern = Pattern.compile(
                "(?m)^(\\s*\"" + Pattern.quote(FORGE_FOLLOW_HERO_KEY) + "\"\\s*=\\s*)(true|false)(\\b)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = followHeroPattern.matcher(content);

        String updated;
        if (matcher.find()) {
            updated = matcher.replaceAll("$1false$3");
        } else {
            updated = appendForgeFollowHeroKey(content);
        }

        if (!updated.equals(original)) {
            Files.writeString(file, updated, StandardCharsets.UTF_8);
            LOGGER.info("[{}] GuardVillagers TOML ajustado: {}=false",
                    RallyOfTheGuard.MOD_ID, FORGE_FOLLOW_HERO_KEY);
        } else {
            LOGGER.info("[{}] GuardVillagers TOML ja permite follow sem Heroi da Vila.", RallyOfTheGuard.MOD_ID);
        }
    }

    private static String removeStaleFabricKey(String content) {
        return Pattern.compile("(?m)^\\s*followHero\\s*=\\s*(true|false)\\s*\\R?", Pattern.CASE_INSENSITIVE)
                .matcher(content)
                .replaceAll("");
    }

    private static String appendForgeFollowHeroKey(String content) {
        String line = "\"" + FORGE_FOLLOW_HERO_KEY + "\" = false" + System.lineSeparator();
        int guardStuffIndex = content.indexOf("[\"guard stuff\"]");
        if (guardStuffIndex < 0) {
            return content + System.lineSeparator() + "[\"guard stuff\"]" + System.lineSeparator() + line;
        }

        int nextSectionIndex = content.indexOf(System.lineSeparator() + "[\"", guardStuffIndex + 1);
        if (nextSectionIndex < 0) {
            return content + System.lineSeparator() + line;
        }

        return content.substring(0, nextSectionIndex)
                + System.lineSeparator()
                + line
                + content.substring(nextSectionIndex);
    }
}
