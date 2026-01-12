package net.wessez.economy.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.LinkedHashMap;
import java.util.Map;

public record BaltopScreenData(Map<String, Double> balances) {

    public static final StreamCodec<RegistryFriendlyByteBuf, BaltopScreenData> PACKET_CODEC = StreamCodec.of(
            BaltopScreenData::write,
            BaltopScreenData::read
    );

    private static void write(RegistryFriendlyByteBuf buf, BaltopScreenData data) {
        buf.writeInt(data.balances.size());
        data.balances.forEach((name, balance) -> {
            buf.writeUtf(name);
            buf.writeDouble(balance);
        });
    }

    private static BaltopScreenData read(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, Double> balances = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String name = buf.readUtf();
            double balance = buf.readDouble();
            balances.put(name, balance);
        }
        return new BaltopScreenData(balances);
    }
}
