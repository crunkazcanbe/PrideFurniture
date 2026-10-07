package com.dogpound.furniture;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Gloss tile (requested feature): a polished floor tile in all 16 dye colours that shows a mirror image of you and
 * any mob standing on it — no shaders. The block itself is a plain full cube; client/GlossReflection draws the
 * reflection and the glaze on top of its top face.
 */
public class BlockGlossTile extends Block {
    public static final PropertyEnum<EnumDyeColor> COLOR = PropertyEnum.create("color", EnumDyeColor.class);

    public BlockGlossTile() {
        super(Material.ROCK, MapColor.QUARTZ);
        setRegistryName(PrideFurniture.ID, "gloss_tile");
        setUnlocalizedName(PrideFurniture.ID + ".gloss_tile");
        setHardness(1.5F);
        setResistance(10F);
        setSoundType(SoundType.STONE);
        setCreativeTab(PrideFurniture.TAB);
        setDefaultState(blockState.getBaseState().withProperty(COLOR, EnumDyeColor.WHITE));
    }

    /** the tile's polished colour: its dye colour lifted a quarter of the way to white (the textures use the same) */
    public static int tint(EnumDyeColor c) {
        int rgb = c.getColorValue();
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        r += (255 - r) / 4; g += (255 - g) / 4; b += (255 - b) / 4;
        return (r << 16) | (g << 8) | b;
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, COLOR); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(COLOR, EnumDyeColor.byMetadata(meta)); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(COLOR).getMetadata(); }
    @Override public int damageDropped(IBlockState s) { return getMetaFromState(s); }
    @Override public MapColor getMapColor(IBlockState s, IBlockAccess w, BlockPos p) { return MapColor.getBlockColor(s.getValue(COLOR)); }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (EnumDyeColor c : EnumDyeColor.values()) items.add(new ItemStack(this, 1, c.getMetadata()));
    }
}
