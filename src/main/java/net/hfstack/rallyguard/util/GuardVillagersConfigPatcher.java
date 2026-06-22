package net.hfstack.rallyguard.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.hfstack.rallyguard.RallyOfTheGuard;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Garante followHero=false no config do Guard Villagers.
 * Suporta JSON (Fabric) e TOML (Forge: guardvillagers-common.toml).
 */
public final class GuardVillagersConfigPatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RallyOfTheGuard.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GuardVillagersConfigPatcher() {
    }

    public static void patchFollowHeroConfig() {
        try {
            Path cfgDir = FMLPaths.CONFIGDIR.get();
            Path json = cfgDir.resolve("guardvillagers.json");
            Path toml = cfgDir.resolve("guardvillagers-common.toml");

            if (Files.exists(json)) {
                patchJson(json);
                return;
            }

            if (Files.exists(toml)) {
                patchToml(toml);
                return;
            }

            LOGGER.info("[{}] Config do GuardVillagers não encontrado ainda (será criado no primeiro run).",
                    RallyOfTheGuard.MOD_ID);
        } catch (Exception e) {
            LOGGER.error("[{}] Falha ao ajustar followHero no GuardVillagers", RallyOfTheGuard.MOD_ID, e);
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
            LOGGER.info("[{}] GuardVillagers JSON já está com followHero=false", RallyOfTheGuard.MOD_ID);
        }
    }

    private static void patchToml(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);

        // troca followHero = true -> false (case-insensitive, preserva espaços)
        Pattern p = Pattern.compile("(?m)^(\\s*followHero\\s*=\\s*)(true)(\\b)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(content);

        if (m.find()) {
            String updated = m.replaceAll("$1false$3");
            Files.writeString(file, updated, StandardCharsets.UTF_8);
            LOGGER.info("[{}] GuardVillagers TOML ajustado: followHero=false", RallyOfTheGuard.MOD_ID);
        } else if (!content.toLowerCase().contains("followhero")) {
            // não existe a chave -> acrescenta no final (fallback simples)
            String updated = content + System.lineSeparator() + "followHero = false" + System.lineSeparator();
            Files.writeString(file, updated, StandardCharsets.UTF_8);
            LOGGER.info("[{}] GuardVillagers TOML: adicionada chave followHero=false no final.", RallyOfTheGuard.MOD_ID);
        } else {
            LOGGER.info("[{}] GuardVillagers TOML já possui followHero=false (ou valor equivalente).",
                    RallyOfTheGuard.MOD_ID);
        }
    }
}
