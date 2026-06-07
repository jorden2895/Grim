package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiActionsF", stableKey = "grim.multiactions.block_and_entity_interact", verboseVersion = 1, description = "Interacting with a block and an entity in the same tick", experimental = true)
public class MultiActionsF extends BlockPlaceCheck {
    public static final VerboseSchema V = VerboseSchema.of("action:vi");

    static final int ACTION_PLACE = 0;
    static final int ACTION_ENTITY = 1;
    static final int ACTION_DIG = 2;

    private final List<FlagData> flags = new ArrayList<>();
    private boolean entity, block;

    public MultiActionsF(GrimPlayer player) {
        super(player);
    }

    static String verbose(int action) {
        return switch (action) {
            case ACTION_PLACE -> "place";
            case ACTION_ENTITY -> "entity";
            case ACTION_DIG -> "dig";
            default -> "unknown";
        };
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        block = true;
        if (entity) {
            String verbose = verbose(ACTION_PLACE);
            if (!player.canSkipTicks()) {
                if (flagAndAlert(V.write(verbose()).vi(ACTION_PLACE), verbose) && shouldModifyPackets() && shouldCancel()) {
                    place.resync();
                }
            } else {
                flags.add(new FlagData(verbose, ACTION_PLACE));
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                || event.getPacketType() == PacketType.Play.Client.ATTACK
                || event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY) {
            entity = true;
            if (block) {
                String verbose = verbose(ACTION_ENTITY);
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(V.write(verbose()).vi(ACTION_ENTITY), verbose) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(new FlagData(verbose, ACTION_ENTITY));
                }
            }
        }

        if (isTickPacket(event.getPacketType())) {
            block = entity = false;
        }
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING || blockBreak.action == DiggingAction.FINISHED_DIGGING) {
            block = true;
            if (entity) {
                String verbose = verbose(ACTION_DIG);
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(V.write(verbose()).vi(ACTION_DIG), verbose) && shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                } else {
                    flags.add(new FlagData(verbose, ACTION_DIG));
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                flagAndAlert(V.write(verbose()).vi(data.action()), data.verbose());
            }
        }

        flags.clear();
    }

    private record FlagData(String verbose, int action) {
    }
}
