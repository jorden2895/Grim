package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(name = "MultiActionsG", stableKey = "grim.multiactions.action_while_rowing", verboseVersion = 1, description = "Attacking or using items while rowing a boat", experimental = true)
public class MultiActionsG extends BlockPlaceCheck {
    public static final VerboseSchema V = VerboseSchema.of("action:vi");

    static final int ACTION_INTERACT = 0;
    static final int ACTION_ATTACK = 1;
    static final int ACTION_SPECTATE_ENTITY = 2;
    static final int ACTION_USE = 3;
    static final int ACTION_PLACE = 4;

    public MultiActionsG(GrimPlayer player) {
        super(player);
    }

    static String verbose(int action) {
        return switch (action) {
            case ACTION_INTERACT -> "interact";
            case ACTION_ATTACK -> "attack";
            case ACTION_SPECTATE_ENTITY -> "spectateEntity";
            case ACTION_USE -> "use";
            case ACTION_PLACE -> "place";
            default -> "unknown";
        };
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        String interact = verbose(ACTION_INTERACT);
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY && isCheckActive()
                && flagAndAlert(V.write(verbose()).vi(ACTION_INTERACT), interact) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }

        String attack = verbose(ACTION_ATTACK);
        if (event.getPacketType() == PacketType.Play.Client.ATTACK && isCheckActive()
                && flagAndAlert(V.write(verbose()).vi(ACTION_ATTACK), attack) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }

        String spectateEntity = verbose(ACTION_SPECTATE_ENTITY);
        if (event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY && isCheckActive()
                && flagAndAlert(V.write(verbose()).vi(ACTION_SPECTATE_ENTITY), spectateEntity) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }

        String use = verbose(ACTION_USE);
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && isCheckActive()
                && flagAndAlert(V.write(verbose()).vi(ACTION_USE), use) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        int action = place.getFace() == BlockFace.OTHER ? ACTION_USE : ACTION_PLACE;
        String verbose = verbose(action);
        if (isCheckActive() && flagAndAlert(V.write(verbose()).vi(action), verbose) && shouldModifyPackets() && shouldCancel()) {
            place.resync();
        }
    }

    public boolean isCheckActive() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && !player.vehicleData.wasVehicleSwitch // one tick off?
                && player.inVehicle() && player.compensatedEntities.self.getRiding().getType().isInstanceOf(EntityTypes.BOAT)
                && (player.vehicleData.nextVehicleForward != 0 || player.vehicleData.nextVehicleHorizontal != 0);
    }
}
