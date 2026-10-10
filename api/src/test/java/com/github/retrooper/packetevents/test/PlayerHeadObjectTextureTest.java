/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.retrooper.packetevents.test;

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.test.base.BaseDummyAPITest;
import com.github.retrooper.packetevents.util.adventure.AdventureNBTSerializer;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.serializer.gson.BackwardCompatUtil;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A rewritten packet must keep the {@code texture} of a player head object component
 * ({@code {"type":"object","player":{"texture":"..."}}}, Minecraft 1.21.9+), through both
 * the NBT and the JSON serializer.
 */
public class PlayerHeadObjectTextureTest extends BaseDummyAPITest {

    private static final Key TEXTURE = Key.key("btc_anim/spinner_3");
    private static final UUID PROFILE_ID = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @BeforeEach
    public void requirePlayerHeadSupport() {
        assumeTrue(BackwardCompatUtil.IS_4_25_0_OR_NEWER, "player head object contents need adventure 4.25.0");
    }

    @ParameterizedTest
    @EnumSource(value = ClientVersion.class, names = {"V_1_21_9", "V_26_3"})
    @DisplayName("texture alone survives an NBT round trip")
    public void textureOnlyRoundTripsThroughNbt(ClientVersion version) {
        Component original = headObject(ObjectContents.playerHead().texture(TEXTURE));

        PlayerHeadObjectContents decoded = nbtRoundTrip(original, version);

        assertEquals(TEXTURE, decoded.texture());
        assertEquals(original, Component.object().contents(decoded).build());
    }

    @ParameterizedTest
    @EnumSource(value = ClientVersion.class, names = {"V_1_21_9", "V_26_3"})
    @DisplayName("texture alone survives a JSON round trip")
    public void textureOnlyRoundTripsThroughJson(ClientVersion version) {
        Component original = headObject(ObjectContents.playerHead().texture(TEXTURE));

        PlayerHeadObjectContents decoded = jsonRoundTrip(original, version);

        assertEquals(TEXTURE, decoded.texture());
        assertEquals(original, Component.object().contents(decoded).build());
    }

    @Test
    @DisplayName("texture is written under player.texture in the NBT form")
    public void textureIsWrittenInsidePlayerCompound() {
        Component original = headObject(ObjectContents.playerHead().texture(TEXTURE));

        NBT serialized = nbtSerializer(ClientVersion.V_26_3).serialize(original, wrapper(ClientVersion.V_26_3));

        NBTCompound player = ((NBTCompound) serialized).getCompoundTagOrNull("player");
        assertNotNull(player, "player compound should be written");
        NBT texture = player.getTagOrNull("texture");
        assertInstanceOf(NBTString.class, texture);
        assertEquals(TEXTURE.asString(), ((NBTString) texture).getValue());
    }

    @ParameterizedTest
    @EnumSource(value = ClientVersion.class, names = {"V_1_21_9", "V_26_3"})
    @DisplayName("texture next to a full profile survives an NBT round trip")
    public void textureWithProfileRoundTripsThroughNbt(ClientVersion version) {
        Component original = headObject(profileWithTexture());

        PlayerHeadObjectContents decoded = nbtRoundTrip(original, version);

        assertProfileWithTexture(decoded);
        assertEquals(original, Component.object().contents(decoded).build());
    }

    @ParameterizedTest
    @EnumSource(value = ClientVersion.class, names = {"V_1_21_9", "V_26_3"})
    @DisplayName("texture next to a full profile survives a JSON round trip")
    public void textureWithProfileRoundTripsThroughJson(ClientVersion version) {
        Component original = headObject(profileWithTexture());

        PlayerHeadObjectContents decoded = jsonRoundTrip(original, version);

        assertProfileWithTexture(decoded);
        assertEquals(original, Component.object().contents(decoded).build());
    }

    @ParameterizedTest
    @EnumSource(value = ClientVersion.class, names = {"V_1_21_9", "V_26_3"})
    @DisplayName("a head without texture still round trips without inventing one")
    public void headWithoutTextureKeepsNoTexture(ClientVersion version) {
        Component original = headObject(ObjectContents.playerHead()
                .name("Notch")
                .id(PROFILE_ID)
                .profileProperty(PlayerHeadObjectContents.property("textures", "e30=", "c2ln")));

        PlayerHeadObjectContents viaNbt = nbtRoundTrip(original, version);
        PlayerHeadObjectContents viaJson = jsonRoundTrip(original, version);

        assertNull(viaNbt.texture());
        assertNull(viaJson.texture());
        assertEquals(original, Component.object().contents(viaNbt).build());
        assertEquals(original, Component.object().contents(viaJson).build());
    }

    @ParameterizedTest
    @EnumSource(value = ClientVersion.class, names = {"V_1_21_9", "V_26_3"})
    @DisplayName("hat=false is kept next to a texture, through NBT and JSON")
    public void hatFalseIsKeptWithTexture(ClientVersion version) {
        Component original = headObject(ObjectContents.playerHead().texture(TEXTURE).hat(false));

        PlayerHeadObjectContents viaNbt = nbtRoundTrip(original, version);
        PlayerHeadObjectContents viaJson = jsonRoundTrip(original, version);

        assertFalse(viaNbt.hat());
        assertFalse(viaJson.hat());
        assertEquals(TEXTURE, viaNbt.texture());
        assertEquals(TEXTURE, viaJson.texture());
    }

    @Test
    @DisplayName("hat defaults to true when it is not written")
    public void hatDefaultsToTrue() {
        Component original = headObject(ObjectContents.playerHead().texture(TEXTURE));

        assertTrue(nbtRoundTrip(original, ClientVersion.V_26_3).hat());
        assertTrue(jsonRoundTrip(original, ClientVersion.V_26_3).hat());
    }

    private static PlayerHeadObjectContents.Builder profileWithTexture() {
        return ObjectContents.playerHead()
                .name("Notch")
                .id(PROFILE_ID)
                .profileProperty(PlayerHeadObjectContents.property("textures", "e30=", "c2ln"))
                .texture(TEXTURE);
    }

    private static void assertProfileWithTexture(PlayerHeadObjectContents decoded) {
        assertEquals("Notch", decoded.name());
        assertEquals(PROFILE_ID, decoded.id());
        assertEquals(1, decoded.profileProperties().size());
        assertEquals("textures", decoded.profileProperties().get(0).name());
        assertEquals("e30=", decoded.profileProperties().get(0).value());
        assertEquals("c2ln", decoded.profileProperties().get(0).signature());
        assertEquals(TEXTURE, decoded.texture());
    }

    private static Component headObject(PlayerHeadObjectContents.Builder contents) {
        return Component.object().contents(contents.build()).build();
    }

    private static AdventureNBTSerializer nbtSerializer(ClientVersion version) {
        return AdventureSerializer.serializer(version).nbt();
    }

    private static PacketWrapper<?> wrapper(ClientVersion version) {
        return PacketWrapper.createDummyWrapper(version);
    }

    private static PlayerHeadObjectContents nbtRoundTrip(Component original, ClientVersion version) {
        AdventureNBTSerializer serializer = nbtSerializer(version);
        PacketWrapper<?> wrapper = wrapper(version);
        NBT serialized = serializer.serialize(original, wrapper);
        return playerHead(serializer.deserialize(serialized, wrapper));
    }

    private static PlayerHeadObjectContents jsonRoundTrip(Component original, ClientVersion version) {
        GsonComponentSerializer gson = AdventureSerializer.serializer(version).gson();
        return playerHead(gson.deserialize(gson.serialize(original)));
    }

    private static PlayerHeadObjectContents playerHead(Component component) {
        ObjectComponent object = assertInstanceOf(ObjectComponent.class, component);
        return assertInstanceOf(PlayerHeadObjectContents.class, object.contents());
    }
}
