package totemresize.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TotemResizePayload() implements CustomPayload {
    public static final CustomPayload.Id<TotemResizePayload> ID = new CustomPayload.Id<>(Identifier.of("totemresize", "client_payload"));
    public static final PacketCodec<RegistryByteBuf, TotemResizePayload> CODEC =
        PacketCodec.unit(new TotemResizePayload());

    @Override
    public Id<TotemResizePayload> getId() {
        return ID;
    }
}
