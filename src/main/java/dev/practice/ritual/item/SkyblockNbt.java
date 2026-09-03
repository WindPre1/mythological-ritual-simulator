package dev.practice.ritual.item;

import dev.practice.ritual.RitualPlugin;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Writes Hypixel-style {@code custom_data:{id:"..."}} so SBO ItemLookup.sbId works.
 * Paper 1.21.11's API does not expose CUSTOM_DATA, so this uses CraftItemStack + NMS.
 */
public final class SkyblockNbt {
    private SkyblockNbt() {}

    public static ItemStack withId(ItemStack bukkit, String id) {
        try {
            Class<?> craft = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            Method asCraftCopy = craft.getMethod("asCraftCopy", ItemStack.class);
            ItemStack copy = (ItemStack) asCraftCopy.invoke(null, bukkit);

            Field handleF = craft.getDeclaredField("handle");
            handleF.setAccessible(true);
            Object nms = handleF.get(copy);

            Class<?> compound = Class.forName("net.minecraft.nbt.CompoundTag");
            Object tag = compound.getConstructor().newInstance();
            Method putString = null;
            for (Method m : compound.getMethods()) {
                if (m.getName().equals("putString") && m.getParameterCount() == 2) {
                    putString = m;
                    break;
                }
            }
            if (putString == null) throw new IllegalStateException("CompoundTag.putString missing");
            putString.invoke(tag, "id", id);

            Class<?> customDataCl = Class.forName("net.minecraft.world.item.component.CustomData");
            Method of = null;
            for (Method m : customDataCl.getMethods()) {
                if ((m.getName().equals("of") || m.getName().equals("copyOf")) && m.getParameterCount() == 1) {
                    of = m;
                    break;
                }
            }
            if (of == null) throw new IllegalStateException("CustomData.of missing");
            Object wrapped = of.invoke(null, tag);

            Class<?> dataComponents = Class.forName("net.minecraft.core.component.DataComponents");
            Object type = dataComponents.getField("CUSTOM_DATA").get(null);

            Class<?> dct = Class.forName("net.minecraft.core.component.DataComponentType");
            Method set = null;
            for (Method m : nms.getClass().getMethods()) {
                if (m.getName().equals("set") && m.getParameterCount() == 2 && dct.isAssignableFrom(m.getParameterTypes()[0])) {
                    set = m;
                    break;
                }
            }
            if (set == null) throw new IllegalStateException("ItemStack.set missing");
            set.invoke(nms, type, wrapped);
            return copy;
        } catch (Throwable t) {
            RitualPlugin.get().getLogger().warning("Could not write custom_data.id=" + id + ": " + t);
            return bukkit;
        }
    }
}
