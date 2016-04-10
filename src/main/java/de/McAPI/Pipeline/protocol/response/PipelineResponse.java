package de.McAPI.Pipeline.protocol.response;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.McAPI.Pipeline.Pipeline;
import de.McAPI.Pipeline.Session;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.world.World;

import java.util.Map;

/**
 * Created by Yonas on 09.04.2016.
 */
public class PipelineResponse {

    private final static Gson gson = new Gson();

    private Pipeline pipeline;
    private Session session;

    private JsonObject response = new JsonObject();

    public PipelineResponse(Pipeline pipeline, Session session) {
        this.pipeline = pipeline;
        this.session = session;
    }

    /**
     * Returns the response as JSON string.
     * @return
     */
    public String getResponse() {
        return this.gson.toJson(this.response);
    }

    /**
     * Call this function to start building the Pipeline response.
     */
    public void build() {

        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Starting to build the PipelineResponse...",
                    this.session.getDebugKey()
            ));
        }

        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Adding players...",
                    this.session.getDebugKey()
            ));
        }
        //player
        JsonArray players = new JsonArray();
        for(Player player : Sponge.getGame().getServer().getOnlinePlayers()) {

            JsonObject entry = new JsonObject();
            entry.addProperty("identifier", player.getIdentifier());
            entry.addProperty("name", player.getName());
            entry.addProperty("ping", player.getConnection().getLatency());
            entry.addProperty("gamemode", player.getGameModeData().get(Keys.GAME_MODE).get().getName());
            entry.addProperty("health", player.getHealthData().getValue(Keys.HEALTH).get().get());
            entry.addProperty("food", player.getFoodData().foodLevel().get());

            // Status
            JsonObject status = new JsonObject();
            status.addProperty("isFlying", player.get(Keys.IS_FLYING).get());
            status.addProperty("isSneaking", player.get(Keys.IS_SNEAKING).get());
            status.addProperty("isSprinting", player.get(Keys.IS_SPRINTING).get());
            entry.add("status", status);

            players.add(entry);


        }
        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Finished adding (%d) players.",
                    this.session.getDebugKey(),
                    players.size()
            ));
        }

        //worlds
        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Adding worlds...",
                    this.session.getDebugKey()
            ));
        }
        JsonArray worlds = new JsonArray();
        for(World world : Sponge.getGame().getServer().getWorlds()) {

            JsonObject entry = new JsonObject();
            entry.addProperty("identifier", world.getUniqueId().toString());
            entry.addProperty("name", world.getName());
            entry.addProperty("difficulty", world.getDifficulty().getName());
            entry.addProperty("dimension", world.getDimension().getName());
            entry.addProperty("generatorType", world.getDimension().getGeneratorType().getName());


            // Game Rules
            JsonArray gameRules = new JsonArray();
            for(Map.Entry<String, String> rule : world.getProperties().getGameRules().entrySet()) {

                JsonObject ruleEntry = new JsonObject();
                ruleEntry.addProperty(rule.getKey(), Boolean.valueOf(rule.getValue()));

                gameRules.add(ruleEntry);
            }
            entry.add("gamesRules", gameRules);

            worlds.add(entry);

        }
        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Finished adding (%d) worlds.",
                    this.session.getDebugKey(),
                    worlds.size()
            ));
        }

        // Plugins
        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Adding plugins...",
                    this.session.getDebugKey()
            ));
        }
        JsonArray plugins = new JsonArray();
        for(PluginContainer pluginContainer : Sponge.getGame().getPluginManager().getPlugins()) {

            JsonObject entry = new JsonObject();
            entry.addProperty("name", pluginContainer.getName());
            entry.addProperty("description", pluginContainer.getDescription().orElse(null));
            entry.addProperty("version", pluginContainer.getVersion().orElse(null));

            // Authors
            JsonArray authors = new JsonArray();
            for(String author : pluginContainer.getAuthors()) {
                authors.add(new JsonPrimitive(author));
            }
            entry.add("authors", authors);

            plugins.add(entry);

        }
        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Finished adding (%d) plugins.",
                    this.session.getDebugKey(),
                    plugins.size()
            ));
        }

        // Platform
        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Adding platform details.",
                    this.session.getDebugKey()
            ));
        }
        JsonObject platform = new JsonObject();
        Platform gamePlatform = Sponge.getGame().getPlatform();

        platform.addProperty("type", gamePlatform.getType().name());
        platform.addProperty("api", gamePlatform.getApi().getVersion().orElse(null));
        platform.addProperty("implementation", gamePlatform.getImplementation().getVersion().orElse(null));
        platform.addProperty("minecraft", gamePlatform.getMinecraftVersion().getName());
        platform.addProperty("online-mode", Sponge.getGame().getServer().getOnlineMode());

        // performance
        JsonObject performance = new JsonObject();
        performance.addProperty("tps", Sponge.getGame().getServer().getTicksPerSecond());

        JsonObject cpu = new JsonObject();
        cpu.addProperty("cores", Runtime.getRuntime().availableProcessors());

        JsonObject memory = new JsonObject();
        memory.addProperty("max", Runtime.getRuntime().maxMemory());
        memory.addProperty("total", Runtime.getRuntime().totalMemory());
        memory.addProperty("free", Runtime.getRuntime().freeMemory());
        memory.addProperty("used", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

        performance.add("cpu", cpu);
        performance.add("memory", memory);

        platform.add("performance", performance);

        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Finished adding platform details.",
                    this.session.getDebugKey()
            ));
        }

        // Debug Information
        this.response.addProperty("debug", this.session.getDebugKey());

        //add
        this.response.add("platform", platform);
        this.response.add("worlds", worlds);
        this.response.add("players", players);
        this.response.add("plugins", plugins);

        if(this.pipeline.isDebug()) {
            this.pipeline.logger().info(String.format(
                    "[%s] Finished PipelineResponse building.",
                    this.session.getDebugKey(),
                    plugins.size()
            ));
        }

    }

}
