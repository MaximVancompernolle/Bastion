package com.mvc.bastion.Filters;

import Xinyuiii.properties.BastionGenerator;
import com.mvc.bastion.Config;
import com.seedfinding.mccore.block.Block;
import com.seedfinding.mccore.block.Blocks;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.math.DistanceMetric;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mcfeature.loot.LootContext;
import com.seedfinding.mcfeature.loot.LootPool;
import com.seedfinding.mcfeature.loot.LootTable;
import com.seedfinding.mcfeature.loot.MCLootTables;
import com.seedfinding.mcfeature.loot.entry.ItemEntry;
import com.seedfinding.mcfeature.loot.function.ApplyDamageFunction;
import com.seedfinding.mcfeature.loot.function.EnchantRandomlyFunction;
import com.seedfinding.mcfeature.loot.function.SetCountFunction;
import com.seedfinding.mcfeature.loot.item.Item;
import com.seedfinding.mcfeature.loot.item.ItemStack;
import com.seedfinding.mcfeature.loot.item.Items;
import com.seedfinding.mcfeature.loot.roll.ConstantRoll;
import com.seedfinding.mcfeature.loot.roll.UniformRoll;
import com.seedfinding.mcfeature.structure.BastionRemnant;
import com.seedfinding.mcfeature.structure.RuinedPortal;
import com.seedfinding.mcfeature.structure.generator.structure.RuinedPortalGenerator;


import java.util.List;

public class StructureFilter {
    private final long structureSeed;
    private final ChunkRand chunkRand;
    private CPos rpLocation;
    private CPos bastionLocation;
    private CPos center;
    private int obsidianInChest;
    private static final LootTable BASTION_TREASURE_CHEST = new LootTable(
            new LootPool(new ConstantRoll(3),
                    new ItemEntry(Items.NETHERITE_INGOT, 15).apply(version -> SetCountFunction.constant(1)),
                    new ItemEntry(Items.ANCIENT_DEBRIS, 10).apply(version -> SetCountFunction.constant(1)),
                    new ItemEntry(Items.NETHERITE_SCRAP, 8).apply(version -> SetCountFunction.constant(1)),
                    new ItemEntry(Items.ANCIENT_DEBRIS, 4).apply(version -> SetCountFunction.constant(2)),
                    new ItemEntry(Items.DIAMOND_SWORD, 6).apply(version -> new ApplyDamageFunction(), version -> new EnchantRandomlyFunction(Items.DIAMOND_SWORD).apply(version)),
                    new ItemEntry(Items.DIAMOND_CHESTPLATE, 6).apply(version -> new ApplyDamageFunction(), version -> new EnchantRandomlyFunction(Items.DIAMOND_CHESTPLATE).apply(version)),
                    new ItemEntry(Items.DIAMOND_HELMET, 6).apply(version -> new ApplyDamageFunction(), version -> new EnchantRandomlyFunction(Items.DIAMOND_HELMET).apply(version)),
                    new ItemEntry(Items.DIAMOND_LEGGINGS, 6).apply(version -> new ApplyDamageFunction(), version -> new EnchantRandomlyFunction(Items.DIAMOND_LEGGINGS).apply(version)),
                    new ItemEntry(Items.DIAMOND_BOOTS, 6).apply(version -> new ApplyDamageFunction(), version -> new EnchantRandomlyFunction(Items.DIAMOND_BOOTS).apply(version)),
                    new ItemEntry(Items.DIAMOND_SWORD, 6),
                    new ItemEntry(Items.DIAMOND_CHESTPLATE, 5),
                    new ItemEntry(Items.DIAMOND_HELMET, 5),
                    new ItemEntry(Items.DIAMOND_BOOTS, 5),
                    new ItemEntry(Items.DIAMOND_LEGGINGS, 5),
                    new ItemEntry(Items.DIAMOND, 5).apply(version -> SetCountFunction.uniform(2.0F, 6.0F)),
                    new ItemEntry(Items.ENCHANTED_GOLDEN_APPLE, 2).apply(version -> SetCountFunction.constant(1))),
            new LootPool(new UniformRoll(3.0F, 4.0F),
                    new ItemEntry(Items.SPECTRAL_ARROW).apply(version -> SetCountFunction.uniform(12.0F, 25.0F)),
                    new ItemEntry(Items.GOLD_BLOCK).apply(version -> SetCountFunction.uniform(2.0F, 5.0F)),
                    new ItemEntry(Items.IRON_BLOCK).apply(version -> SetCountFunction.uniform(2.0F, 5.0F)),
                    new ItemEntry(Items.GOLD_INGOT).apply(version -> SetCountFunction.uniform(3.0F, 9.0F)),
                    new ItemEntry(Items.IRON_INGOT).apply(version -> SetCountFunction.uniform(3.0F, 9.0F)),
                    new ItemEntry(Items.CRYING_OBSIDIAN).apply(version -> SetCountFunction.uniform(3.0F, 5.0F)),
                    new ItemEntry(Items.QUARTZ).apply(version -> SetCountFunction.uniform(8.0F, 23.0F)),
                    new ItemEntry(Items.GILDED_BLACKSTONE).apply(version -> SetCountFunction.uniform(5.0F, 15.0F)),
                    new ItemEntry(Items.MAGMA_CREAM).apply(version -> SetCountFunction.uniform(3.0F, 8.0F)))
    );

    public StructureFilter(long structureSeed, ChunkRand chunkRand) {
        this.structureSeed = structureSeed;
        this.chunkRand = chunkRand;
    }

    public boolean filterStructures() {
        return filterBastion() &&
                filterRuinedPortal() &&
                hasRpLoot() &&
                hasTreasureLoot() &&
                isDoubleChest() &&
                isCompletable();
    }

    public boolean filterBastion() {
        BastionRemnant bastion = new BastionRemnant(Config.VERSION);
        bastionLocation = bastion.getInRegion(structureSeed, 0, 0, chunkRand);

        if (bastionLocation == null) {
            return false;
        }
        if (bastionLocation.getMagnitudeSq() > Config.BASTION_MAX_DIST) {
            return false;
        }
        chunkRand.setCarverSeed(structureSeed, bastionLocation.getX(), bastionLocation.getZ(), Config.VERSION);
        center = getBottomTreasureCPos(chunkRand.nextInt(4), bastionLocation);

        return chunkRand.nextInt(4) == 2;
    }

    public boolean isDoubleChest() {
        BastionGenerator bastionGenerator = new BastionGenerator(Config.VERSION);
        bastionGenerator.generate(structureSeed, bastionLocation);

        for (BastionGenerator.Piece piece : bastionGenerator.getPieces()) {
            if (piece.getName().contains("treasure/bases/centers/center_1")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTreasureLoot() {
        chunkRand.setDecoratorSeed(structureSeed, center.getX() << 4, center.getZ() << 4, 40012, Config.VERSION);
        LootTable lootTable = BASTION_TREASURE_CHEST;
        lootTable.apply(Config.VERSION);

        int diamondCount = 0;
        int ironCount = 0;
        boolean hasNetherite = false;

        for (int i = 1; i <= 2; i++) {
            LootContext lootContext = new LootContext(chunkRand.nextLong(), Config.VERSION);
            List<ItemStack> chest = lootTable.generate(lootContext);

            for (ItemStack itemStack : chest) {
                if (itemStack.getItem().equals(Items.DIAMOND)) {
                    diamondCount += itemStack.getCount();
                }
                if (itemStack.getItem().equals(Items.IRON_INGOT)) {
                    ironCount += itemStack.getCount();
                }
                if (itemStack.getItem().equals(Items.IRON_BLOCK)) {
                    ironCount += (itemStack.getCount() * 9);
                }
                if (!hasNetherite && itemStack.getItem().equals(Items.NETHERITE_INGOT)) {
                    hasNetherite = true;
                }
            }
        }
        return hasNetherite && diamondCount >= 6 && ironCount >= 5;
    }

    public CPos getBottomTreasureCPos(int rotation, CPos bastionLocation) {
        return switch (rotation) {
            case 0 -> bastionLocation.add(1, 1);
            case 1 -> bastionLocation.add(-2, 1);
            case 2 -> bastionLocation.add(-2, -2);
            case 3 -> bastionLocation.add(1, -2);
            default -> null;
        };
    }

    public boolean filterRuinedPortal() {
        RuinedPortal rp = new RuinedPortal(Dimension.OVERWORLD, Config.VERSION);
        rpLocation = rp.getInRegion(structureSeed, 0, 0, chunkRand);
        chunkRand.setCarverSeed(structureSeed, rpLocation.getX(), rpLocation.getZ(), Config.VERSION);

        if (chunkRand.nextFloat() < 0.50F) {
            return false;
        }
        return rpLocation.distanceTo(new CPos(center.getX() << 3, center.getZ() << 3), DistanceMetric.EUCLIDEAN_SQ) < Config.RP_MAX_DIST;
    }

    public boolean isCompletable() {
        RuinedPortalGenerator rpGenerator = new RuinedPortalGenerator(Config.VERSION);

        if (!rpGenerator.generate(structureSeed, Dimension.OVERWORLD, rpLocation.getX(), rpLocation.getZ())) {
            return false;
        }
        if (rpGenerator.getType().equals("portal_10")) {
            return false;
        }
        List<Pair<Block, BPos>> minimalPortal = rpGenerator.getMinimalPortal();

        for (Pair<Block, BPos> pair : minimalPortal) {
            if (pair.getFirst().equals(Blocks.CRYING_OBSIDIAN)) {
                return false;
            }
        }
        return minimalPortal.size() + obsidianInChest >= 10;
    }

    public boolean hasRpLoot() {
        chunkRand.setDecoratorSeed(structureSeed, rpLocation.getX() << 4, rpLocation.getZ() << 4, 40005, Config.VERSION);
        LootContext lootContext = new LootContext(chunkRand.nextLong(), Config.VERSION);
        LootTable lootTable = MCLootTables.RUINED_PORTAL_CHEST.get();
        lootTable.apply(Config.VERSION);
        List<ItemStack> items = lootTable.generate(lootContext);

        if (items.isEmpty()) {
            return false;
        }
        boolean hasLight = false;
        boolean hasAxe = false;
        boolean hasPickaxe = false;
        obsidianInChest = 0;

        for (ItemStack stack : items) {
            Item item = stack.getItem();

            if (item.equals(Items.OBSIDIAN)) {
                obsidianInChest = stack.getCount();
            }
            if (!hasLight && (item.equals(Items.FLINT_AND_STEEL) || item.equals(Items.FIRE_CHARGE))) {
                hasLight = true;
            }
            if (!hasAxe && item.getName().equals("golden_axe")) {
                hasAxe = true;
            }
            if (!hasPickaxe && item.getName().equals("golden_pickaxe")) {
                hasPickaxe = true;
            }
            if ((obsidianInChest > 0) && hasLight && hasAxe && hasPickaxe) {
                return true;
            }
        }
        return false;
    }
}