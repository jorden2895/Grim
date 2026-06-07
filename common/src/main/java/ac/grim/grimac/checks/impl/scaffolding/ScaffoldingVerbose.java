package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.storage.verbose.VerboseBuf;
import ac.grim.grimac.api.storage.verbose.VerboseFormatter;
import ac.grim.grimac.api.storage.verbose.VerboseRenderContext;
import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.api.storage.verbose.VerboseSink;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.internal.storage.checks.CheckRegistry;
import ac.grim.grimac.internal.storage.verbose.VerboseRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ScaffoldingVerbose {
    private ScaffoldingVerbose() {
    }

    public static void register(@NotNull VerboseRegistry registry, @NotNull CheckRegistry checks) {
        registerStructured(registry, checks, DuplicateRotPlace.class, DuplicateRotPlace.V,
                formatter(DuplicateRotPlace.V, ScaffoldingVerbose::renderDuplicateRotPlace));
        registerStructured(registry, checks, FabricatedPlace.class, FabricatedPlace.V,
                formatter(FabricatedPlace.V, (in, ctx, out) ->
                        out.text(String.format("cursor=%s limit=%.16f", in.rstr(), in.rf64()))));
        registerStructured(registry, checks, InvalidPlaceB.class, InvalidPlaceB.V,
                formatter(InvalidPlaceB.V, (in, ctx, out) -> out.text("direction=").num(in.rzz())));
        registerStructured(registry, checks, MultiPlace.class, MultiPlace.V,
                formatter(MultiPlace.V, ScaffoldingVerbose::renderMultiPlace));
        registerStructured(registry, checks, RotationPlace.class, RotationPlace.V,
                formatter(RotationPlace.V, (in, ctx, out) -> out.text(in.rbool() ? "pre-flying" : "post-flying")));
    }

    private static void renderDuplicateRotPlace(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        float x = (float) in.rf64();
        double xdots = in.rf64();
        float y = (float) in.rf64();
        out.text("x=").num(x).text(" xdots=").num(xdots).text(" y=").num(y);
    }

    private static void renderMultiPlace(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        out.text("face=").text(in.rstr())
                .text(", lastFace=").text(in.rstr())
                .text(", cursor=").text(in.rstr())
                .text(", lastCursor=").text(in.rstr())
                .text(", pos=").text(in.rstr())
                .text(", lastPos=").text(in.rstr());
    }

    private static void registerStructured(
            @NotNull VerboseRegistry registry,
            @NotNull CheckRegistry checks,
            @NotNull Class<? extends Check> checkClass,
            @NotNull VerboseSchema schema,
            @NotNull VerboseFormatter formatter) {
        registerSchema(registry, checks, checkClass, schema);
        registerFormatter(registry, checkClass, formatter);
    }

    private static void registerSchema(
            @NotNull VerboseRegistry registry,
            @NotNull CheckRegistry checks,
            @NotNull Class<? extends Check> checkClass,
            @NotNull VerboseSchema schema) {
        CheckData data = checkData(checkClass);
        if (data.verboseVersion() < 1) {
            throw new IllegalStateException(checkClass.getName() + " is missing verboseVersion");
        }
        if (schema.version() != data.verboseVersion()) {
            throw new IllegalStateException(checkClass.getName() + " verbose schema v"
                    + schema.version() + " does not match @CheckData verboseVersion="
                    + data.verboseVersion());
        }

        checks.intern(data.stableKey(), data.name(), data.description(), safePluginVersion());
        registry.register(data.stableKey(), schema);
    }

    private static void registerFormatter(
            @NotNull VerboseRegistry registry,
            @NotNull Class<? extends Check> checkClass,
            @NotNull VerboseFormatter formatter) {
        CheckData data = checkData(checkClass);
        if (formatter.version() != data.verboseVersion()) {
            throw new IllegalStateException(checkClass.getName() + " verbose formatter v"
                    + formatter.version() + " does not match @CheckData verboseVersion="
                    + data.verboseVersion());
        }

        registry.registerFormatter(data.stableKey(), formatter);
    }

    private static @NotNull CheckData checkData(@NotNull Class<? extends Check> checkClass) {
        CheckData data = checkClass.getAnnotation(CheckData.class);
        if (data == null) {
            throw new IllegalStateException(checkClass.getName() + " is missing @CheckData");
        }
        if (data.stableKey().isBlank()) {
            throw new IllegalStateException(checkClass.getName() + " is missing a stableKey");
        }
        return data;
    }

    private static @Nullable String safePluginVersion() {
        try {
            return GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @NotNull VerboseFormatter formatter(
            @NotNull VerboseSchema schema,
            @NotNull Renderer renderer) {
        return new VerboseFormatter() {
            @Override
            public int version() {
                return schema.version();
            }

            @Override
            public void render(
                    @NotNull VerboseBuf in,
                    @NotNull VerboseRenderContext ctx,
                    @NotNull VerboseSink out) {
                renderer.render(in, ctx, out);
            }
        };
    }

    private interface Renderer {
        void render(
                @NotNull VerboseBuf in,
                @NotNull VerboseRenderContext ctx,
                @NotNull VerboseSink out);
    }
}
