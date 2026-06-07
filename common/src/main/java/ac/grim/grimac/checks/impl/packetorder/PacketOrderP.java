package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import it.unimi.dsi.fastutil.ints.IntArrayList;

@CheckData(name = "PacketOrderP", stableKey = "grim.packetorder.transaction_response_order", experimental = true, verboseVersion = 1)
public class PacketOrderP extends Check implements PacketCheck {
    public static final VerboseSchema V = VerboseSchema.of("kind:vi", "packetType:str");

    static final int KIND_INVALID_RESPONSE = 0;
    static final int KIND_SKIPPED_RESPONSE = 1;

    public PacketOrderP(final GrimPlayer player) {
        super(player);
    }

    private byte trimTimer; // let the list shrink eventually
    private final IntArrayList transactions = new IntArrayList(0);

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHUNK_BATCH_ACK) {
            if (!transactions.rem(player.getLastTransactionReceived())) {
                String verbose = verbose(KIND_INVALID_RESPONSE, "");
                flagAndAlert(V.write(verbose()).vi(KIND_INVALID_RESPONSE).str(""), verbose);
            }
        } else if (!isAsync(event.getPacketType()) && !isTransaction(event.getPacketType())) {
            if (transactions.rem(player.getLastTransactionReceived())) {
                String packetType = event.getPacketType().toString();
                String verbose = verbose(KIND_SKIPPED_RESPONSE, packetType);
                flagAndAlert(V.write(verbose()).vi(KIND_SKIPPED_RESPONSE).str(packetType), verbose);
            }
        }
    }

    static String verbose(int kind, String packetType) {
        return switch (kind) {
            case KIND_INVALID_RESPONSE -> "invalid response";
            case KIND_SKIPPED_RESPONSE -> "skipped response, type=" + packetType;
            default -> "unknown";
        };
    }

    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CHUNK_BATCH_END) {
            boolean sendingBundlePacket = player.packetStateData.sendingBundlePacket;
            if (!sendingBundlePacket) player.user.sendPacket(new WrapperPlayServerBundle());

            player.sendTransaction();
            int transaction = player.getLastTransactionSent();
            transactions.add(transaction);
            if (++trimTimer == 0) transactions.trim();
            player.addRealTimeTaskNext(() -> {
                if (transactions.rem(transaction)) {
                    String packetType = "TRANSACTION";
                    String verbose = verbose(KIND_SKIPPED_RESPONSE, packetType);
                    flagAndAlert(V.write(verbose()).vi(KIND_SKIPPED_RESPONSE).str(packetType), verbose);
                }
            });

            if (!sendingBundlePacket) {
                event.getTasksAfterSend().add(() -> player.user.sendPacket(new WrapperPlayServerBundle()));
            }
        }
    }
}
