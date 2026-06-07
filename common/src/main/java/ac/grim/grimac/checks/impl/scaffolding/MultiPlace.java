package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiPlace", stableKey = "grim.scaffolding.multi_place", verboseVersion = 1, description = "Placed multiple blocks in a tick", experimental = true)
public class MultiPlace extends BlockPlaceCheck {
    public static final VerboseSchema V = VerboseSchema.of(
            "face:str", "lastFace:str", "cursor:str", "lastCursor:str", "pos:str", "lastPos:str");

    private final List<FlagData> flags = new ArrayList<>();
    private boolean hasPlaced;
    private BlockFace lastFace;
    private Vector3f lastCursor;
    private Vector3i lastPos;

    public MultiPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        final BlockFace face = place.getFace();
        final Vector3f cursor = place.cursor;
        final Vector3i pos = place.position;

        if (hasPlaced && (face != lastFace || !cursor.equals(lastCursor) || !pos.equals(lastPos))) {
            final String faceName = String.valueOf(face);
            final String lastFaceName = String.valueOf(lastFace);
            final String cursorText = MessageUtil.toUnlabledString(cursor);
            final String lastCursorText = MessageUtil.toUnlabledString(lastCursor);
            final String posText = MessageUtil.toUnlabledString(pos);
            final String lastPosText = MessageUtil.toUnlabledString(lastPos);
            if (!player.canSkipTicks()) {
                if (flagAndAlert(V.write(verbose()).str(faceName).str(lastFaceName).str(cursorText).str(lastCursorText).str(posText).str(lastPosText))
                        && shouldModifyPackets() && shouldCancel()) {
                    place.resync();
                }
            } else {
                flags.add(new FlagData(faceName, lastFaceName, cursorText, lastCursorText, posText, lastPosText));
            }
        }

        lastFace = face;
        lastCursor = cursor;
        lastPos = pos;
        hasPlaced = true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            hasPlaced = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                flagAndAlert(V.write(verbose()).str(data.face()).str(data.lastFace()).str(data.cursor()).str(data.lastCursor()).str(data.pos()).str(data.lastPos()));
            }
        }

        flags.clear();
    }

    private record FlagData(
            String face,
            String lastFace,
            String cursor,
            String lastCursor,
            String pos,
            String lastPos) {
    }
}
