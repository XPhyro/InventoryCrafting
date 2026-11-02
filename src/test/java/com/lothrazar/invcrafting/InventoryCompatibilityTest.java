package com.lothrazar.invcrafting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * Runs against Minecraft classes transformed by the NeoForge JUnit launcher.
 * Only the player/world services are mocked; menus, slots, inventory, equipment,
 * click handling, recipe placement, and synchronisation use the real game code.
 */
class InventoryCompatibilityTest {

  private static final int[] GRID_MENU_IDS = {1, 2, 46, 3, 4, 47, 48, 49, 50};
  private Player player;
  private Inventory inventory;
  private EntityEquipment equipment;
  private InventoryMenu menu;

  @BeforeAll
  static void bindVanillaItemComponents() throws ReflectiveOperationException {
    // FML's test bootstrap registers items, but a real server also binds their
    // default components when loading registries. 26.3 split the lookup method
    // into world and reloadable variants; 26.2 still calls it createLookup.
    java.lang.reflect.Method createLookup;
    try {
      createLookup = VanillaRegistries.class.getMethod("createWorldLookup");
    }
    catch (NoSuchMethodException ignored) {
      createLookup = VanillaRegistries.class.getMethod("createLookup");
    }
    boolean runningInIde = SharedConstants.IS_RUNNING_IN_IDE;
    try {
      // RegistrySetBuilder's bootstrap-only empty tag sets intentionally lack
      // value equality. Use the production component-binding checks here;
      // restore development validation before running any inventory operations.
      SharedConstants.IS_RUNNING_IN_IDE = false;
      HolderLookup.Provider registries = (HolderLookup.Provider) createLookup.invoke(null);
      if (createLookup.getName().equals("createWorldLookup")) {
        registries = (HolderLookup.Provider) VanillaRegistries.class
            .getMethod("createReloadableLookup", HolderLookup.Provider.class).invoke(null, registries);
      }
      BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(components -> components.apply());
    }
    finally {
      SharedConstants.IS_RUNNING_IN_IDE = runningInIde;
    }
  }

  @BeforeEach
  void createMenu() {
    Level level = mock(Level.class);
    when(level.isClientSide()).thenReturn(true);
    when(level.enabledFeatures()).thenReturn(FeatureFlags.DEFAULT_FLAGS);
    player = mock(Player.class);
    equipment = new EntityEquipment();
    inventory = new Inventory(player, equipment);
    when(player.level()).thenReturn(level);
    when(player.getInventory()).thenReturn(inventory);
    when(player.typeHolder()).thenReturn(EntityTypes.PLAYER.builtInRegistryHolder());
    when(player.canUseSlot(any())).thenReturn(true);
    when(player.getEquipmentSlotForItem(any())).thenCallRealMethod();
    when(player.isEquippableInSlot(any(), any())).thenCallRealMethod();
    when(player.getItemBySlot(any())).thenAnswer(call -> equipment.get(call.getArgument(0)));
    menu = new InventoryMenu(inventory, false, player);
    player.containerMenu = menu;
  }

  @Test
  void preservesVanillaMenuAndPlayerSlotIds() {
    assertEquals(InventoryMenu.class, menu.getClass());
    assertSame(inventory, player.getInventory());
    assertEquals(51, menu.slots.size());
    assertEquals(3, menu.getGridWidth());
    assertEquals(3, menu.getGridHeight());
    assertEquals(3, menu.getCraftSlots().getWidth());
    assertEquals(3, menu.getCraftSlots().getHeight());
    assertEquals(9, menu.getCraftSlots().getContainerSize());

    for (int id = 0; id < menu.slots.size(); id++) {
      assertEquals(id, menu.getSlot(id).index, "menu slot " + id);
    }
    for (int id = 5; id < 9; id++) {
      assertTrue(menu.getSlot(id) instanceof ArmorSlot);
      assertSame(inventory, menu.getSlot(id).container);
      assertEquals(44 - id, menu.getSlot(id).getContainerSlot());
    }
    for (int id = 9; id < 36; id++) {
      assertSame(inventory, menu.getSlot(id).container);
      assertEquals(id, menu.getSlot(id).getContainerSlot());
    }
    for (int id = 36; id < 45; id++) {
      assertSame(inventory, menu.getSlot(id).container);
      assertEquals(id - 36, menu.getSlot(id).getContainerSlot());
    }
    assertSame(inventory, menu.getSlot(45).container);
    assertEquals(40, menu.getSlot(45).getContainerSlot());
  }

  @Test
  void exposesAllCraftingSlotsInRowMajorOrder() {
    List<Slot> inputs = menu.getInputGridSlots();
    assertEquals(9, inputs.size());
    for (int grid = 0; grid < 9; grid++) {
      Slot slot = inputs.get(grid);
      assertSame(menu.getSlot(GRID_MENU_IDS[grid]), slot);
      assertSame(menu.getCraftSlots(), slot.container);
      assertEquals(grid, slot.getContainerSlot());
      assertEquals(82 + (grid % 3) * 18, slot.x);
      assertEquals(8 + (grid / 3) * 18, slot.y);
      slot.set(new ItemStack(Items.STONE, grid + 1));
      assertEquals(grid + 1, menu.getCraftSlots().getItem(grid).getCount());
    }
  }

  @Test
  void jeiTransferUsesNineInputsAndOnlyVanillaStorage() {
    Transfer transfer = new Transfer();
    assertTrue(transfer.canHandle(menu, null));
    assertEquals(menu.getInputGridSlots(), transfer.getRecipeSlots(menu, null));
    List<Slot> storage = transfer.getInventorySlots(menu, null);
    assertEquals(36, storage.size());
    for (int index = 0; index < storage.size(); index++) {
      assertSame(menu.getSlot(9 + index), storage.get(index));
      assertSame(inventory, storage.get(index).container);
    }
    when(player.isCreative()).thenReturn(true);
    assertFalse(transfer.canHandle(menu, null));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 4, 8})
  void elytraKeyHotbarSwapUsesChestSlotSix(int hotbar) {
    ItemStack elytra = new ItemStack(Items.ELYTRA);
    ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
    inventory.setItem(hotbar, elytra);
    equipment.set(EquipmentSlot.CHEST, chestplate);

    menu.clicked(6, hotbar, ContainerInput.SWAP, player);

    assertSame(elytra, equipment.get(EquipmentSlot.CHEST));
    assertSame(elytra, inventory.getItem(38));
    assertSame(elytra, menu.getSlot(6).getItem());
    assertSame(chestplate, inventory.getItem(hotbar));
    assertTrue(menu.getCraftSlots().isEmpty());
    verify(player).onEquipItem(EquipmentSlot.CHEST, chestplate, elytra);
  }

  @ParameterizedTest
  @ValueSource(ints = {9, 20, 35})
  void elytraKeyInventorySwapUsesVanillaPickupSequence(int inventorySlot) {
    ItemStack elytra = new ItemStack(Items.ELYTRA);
    ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
    inventory.setItem(inventorySlot, elytra);
    equipment.set(EquipmentSlot.CHEST, chestplate);

    menu.clicked(inventorySlot, 0, ContainerInput.PICKUP, player);
    menu.clicked(6, 0, ContainerInput.PICKUP, player);
    menu.clicked(inventorySlot, 0, ContainerInput.PICKUP, player);

    assertTrue(equipment.get(EquipmentSlot.CHEST).is(Items.ELYTRA));
    assertTrue(inventory.getItem(inventorySlot).is(Items.DIAMOND_CHESTPLATE));
    assertTrue(menu.getCarried().isEmpty());
    assertTrue(menu.getCraftSlots().isEmpty());
  }

  @Test
  void armourAndOffhandRetainVanillaValidationAndEquipCallbacks() {
    Slot chest = menu.getSlot(6);
    assertFalse(chest.mayPlace(new ItemStack(Items.STONE)));
    assertTrue(chest.mayPlace(new ItemStack(Items.ELYTRA)));
    assertTrue(chest.mayPlace(new ItemStack(Items.DIAMOND_CHESTPLATE)));
    assertEquals(1, chest.getMaxStackSize());

    when(player.canUseSlot(EquipmentSlot.CHEST)).thenReturn(false);
    assertFalse(chest.isActive());
    when(player.canUseSlot(EquipmentSlot.CHEST)).thenReturn(true);
    assertTrue(chest.isActive());

    ItemStack shield = new ItemStack(Items.SHIELD);
    menu.getSlot(45).setByPlayer(shield);
    assertSame(shield, equipment.get(EquipmentSlot.OFFHAND));
    assertSame(shield, inventory.getItem(40));
    verify(player).onEquipItem(EquipmentSlot.OFFHAND, ItemStack.EMPTY, shield);
  }

  @Test
  void vanillaHotbarSwapsDoNotMoveCraftingOrArmourItems() {
    for (int i = 0; i < 9; i++) {
      inventory.setItem(i, new ItemStack(Items.STONE, i + 1));
    }
    inventory.setItem(20, new ItemStack(Items.DIRT, 32));
    menu.getInputGridSlots().get(8).set(new ItemStack(Items.DIAMOND, 3));
    ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
    equipment.set(EquipmentSlot.HEAD, helmet);

    menu.clicked(20, 8, ContainerInput.SWAP, player);

    assertTrue(inventory.getItem(8).is(Items.DIRT));
    assertEquals(32, menu.getSlot(44).getItem().getCount());
    assertTrue(inventory.getItem(20).is(Items.STONE));
    assertEquals(9, inventory.getItem(20).getCount());
    for (int i = 0; i < 8; i++) {
      assertTrue(menu.getSlot(36 + i).getItem().is(Items.STONE));
      assertEquals(i + 1, menu.getSlot(36 + i).getItem().getCount());
    }
    assertSame(helmet, menu.getSlot(5).getItem());
    assertEquals(3, menu.getCraftSlots().getItem(8).getCount());
  }

  @Test
  void pickBlockBroadcastsTheCorrectHotbarAndInventorySlotIds() {
    for (int i = 0; i < 9; i++) {
      inventory.setItem(i, new ItemStack(Items.STONE, i + 1));
    }
    inventory.setSelectedSlot(4);
    inventory.setItem(20, new ItemStack(Items.DIRT, 17));
    ContainerSynchronizer synchronizer = mock(ContainerSynchronizer.class);
    when(synchronizer.createSlot()).thenAnswer(ignored -> new SnapshotRemoteSlot());
    menu.setSynchronizer(synchronizer);
    clearInvocations(synchronizer);

    // ServerGamePacketListenerImpl.tryPickItem uses these two operations when
    // the picked block already exists in the main inventory.
    inventory.pickSlot(20);
    menu.broadcastChanges();

    assertEquals(4, inventory.getSelectedSlot());
    assertTrue(inventory.getSelectedItem().is(Items.DIRT));
    verify(synchronizer).sendSlotChange(eq(menu), eq(40), argThat(stack -> stack.is(Items.DIRT) && stack.getCount() == 17));
    verify(synchronizer).sendSlotChange(eq(menu), eq(20), argThat(stack -> stack.is(Items.STONE) && stack.getCount() == 5));
    verify(synchronizer, times(2)).sendSlotChange(eq(menu), anyInt(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 46, 47, 48, 49, 50})
  void shiftClickingAnyCraftingInputReturnsItToStorage(int slotId) {
    menu.getSlot(slotId).set(new ItemStack(Items.DIAMOND_CHESTPLATE));

    ItemStack moved = menu.quickMoveStack(player, slotId);

    assertTrue(moved.is(Items.DIAMOND_CHESTPLATE));
    assertTrue(menu.getSlot(slotId).getItem().isEmpty());
    assertTrue(inventory.getItem(9).is(Items.DIAMOND_CHESTPLATE));
    assertTrue(equipment.get(EquipmentSlot.CHEST).isEmpty(), "crafting inputs must not auto-equip");
  }

  @Test
  void creativeModeHidesCraftingInputsWithoutChangingMenuOrSlots() {
    List<Slot> slots = List.copyOf(menu.slots);
    assertTrue(menu.getInputGridSlots().stream().allMatch(Slot::isActive));
    when(player.isCreative()).thenReturn(true);
    assertTrue(menu.getInputGridSlots().stream().noneMatch(Slot::isActive));
    assertTrue(menu.getSlot(6).isActive());
    assertTrue(menu.getSlot(36).isActive());
    assertTrue(menu.getSlot(45).isActive());
    assertEquals(slots, menu.slots);
    assertEquals(51, menu.slots.size());
    when(player.isCreative()).thenReturn(false);
    assertTrue(menu.getInputGridSlots().stream().allMatch(Slot::isActive));
    assertEquals(slots, menu.slots);
  }

  @ParameterizedTest
  @ValueSource(ints = {46, 47, 48, 49, 50})
  void creativeTrashPacketsClearAllExtraCraftingInputs(int slotId) throws ReflectiveOperationException {
    ServerGamePacketListenerImpl handler = creativePacketHandler();
    menu.getSlot(slotId).set(new ItemStack(Items.STONE, 5));

    handler.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(slotId, ItemStack.EMPTY));

    assertTrue(menu.getSlot(slotId).getItem().isEmpty());
  }

  @Test
  void creativePacketExtensionRejectsNonemptySurvivalAndUnrelatedSlots() throws ReflectiveOperationException {
    ServerGamePacketListenerImpl handler = creativePacketHandler();
    menu.getSlot(46).set(new ItemStack(Items.STONE, 5));
    handler.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(46, new ItemStack(Items.DIAMOND)));
    assertEquals(5, menu.getSlot(46).getItem().getCount());
    assertTrue(menu.getSlot(46).getItem().is(Items.STONE));

    when(handler.player.hasInfiniteMaterials()).thenReturn(false);
    handler.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(46, ItemStack.EMPTY));
    assertEquals(5, menu.getSlot(46).getItem().getCount());
    when(handler.player.hasInfiniteMaterials()).thenReturn(true);

    SimpleContainer otherModStorage = new SimpleContainer(new ItemStack(Items.DIAMOND, 3));
    Slot unrelated = new Slot(otherModStorage, 0, 0, 0);
    var addSlot = AbstractContainerMenu.class.getDeclaredMethod("addSlot", Slot.class);
    addSlot.setAccessible(true);
    addSlot.invoke(menu, unrelated);
    handler.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(unrelated.index, ItemStack.EMPTY));
    assertEquals(3, otherModStorage.getItem(0).getCount());
    assertDoesNotThrow(() -> handler.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(32000, ItemStack.EMPTY)));
  }

  @Test
  void slotTrackersAndInitialSynchronisationMatchActualSlots() throws ReflectiveOperationException {
    assertEquals(51, privateListSize("lastSlots"));
    assertEquals(51, privateListSize("remoteSlots"));
    ContainerSynchronizer synchronizer = mock(ContainerSynchronizer.class);
    when(synchronizer.createSlot()).thenAnswer(ignored -> mock(RemoteSlot.class));
    menu.setSynchronizer(synchronizer);
    ArgumentCaptor<List<ItemStack>> initial = ArgumentCaptor.forClass(List.class);
    verify(synchronizer).sendInitialData(eq(menu), initial.capture(), any(), any());
    assertEquals(51, initial.getValue().size());

    menu.getSlot(50).set(new ItemStack(Items.DIAMOND, 7));
    menu.broadcastChanges();
    verify(synchronizer).sendSlotChange(eq(menu), eq(50), argThat(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 7));
  }

  @Test
  void fullInventoryPacketUpdatesEverySlotAndCursor() {
    List<ItemStack> packet = new ArrayList<>();
    for (int id = 0; id < 51; id++) {
      packet.add(new ItemStack(Items.STONE, id + 1));
    }
    ItemStack cursor = new ItemStack(Items.DIRT, 5);
    menu.initializeContents(17, packet, cursor);
    for (int id = 0; id < 51; id++) {
      assertEquals(id + 1, menu.getSlot(id).getItem().getCount(), "packet slot " + id);
    }
    assertSame(cursor, menu.getCarried());
    assertEquals(17, menu.getStateId());
  }

  @Test
  void inheritedRecipePlacementUsesTheActualThreeByThreeGrid() {
    List<Item> ingredients = List.of(Items.STONE, Items.DIRT, Items.COBBLESTONE,
        Items.OAK_PLANKS, Items.GLASS, Items.SAND, Items.GRAVEL, Items.BRICKS, Items.TORCH);
    for (int i = 0; i < ingredients.size(); i++) {
      inventory.setItem(9 + i, new ItemStack(ingredients.get(i)));
    }
    RecipeHolder<CraftingRecipe> recipe = threeByThreeRecipe(ingredients);

    RecipeBookMenu.PostPlaceAction result = menu.handlePlacement(false, false, recipe, mock(ServerLevel.class), inventory);

    assertEquals(RecipeBookMenu.PostPlaceAction.NOTHING, result);
    for (int grid = 0; grid < 9; grid++) {
      assertTrue(menu.getCraftSlots().getItem(grid).is(ingredients.get(grid)), "grid cell " + grid);
      assertTrue(menu.getSlot(GRID_MENU_IDS[grid]).getItem().is(ingredients.get(grid)));
      assertEquals(1, menu.getCraftSlots().getItem(grid).getCount());
      assertTrue(inventory.getItem(9 + grid).isEmpty());
    }
    assertTrue(equipment.isEmpty());
  }

  @Test
  void inheritedCraftingContentsAndRecipeClearUseAllNineInputs() {
    RecipeHolder<CraftingRecipe> recipe = threeByThreeRecipe(java.util.Collections.nCopies(9, Items.STONE));
    for (Slot slot : menu.getInputGridSlots()) {
      slot.set(new ItemStack(Items.STONE));
    }
    StackedItemContents contents = new StackedItemContents();
    menu.fillCraftSlotsStackedContents(contents);
    assertTrue(contents.canCraft(recipe.value(), null));

    RecipeHolder<CraftingRecipe> unavailable = threeByThreeRecipe(java.util.Collections.nCopies(9, Items.DIAMOND));
    assertEquals(RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE,
        menu.handlePlacement(false, false, unavailable, mock(ServerLevel.class), inventory));
    assertTrue(menu.getCraftSlots().isEmpty());
    assertEquals(9, countStored(Items.STONE));
    assertTrue(equipment.isEmpty());
  }

  @Test
  void closingMenuReturnsEveryInputAndCursorExactlyOnce() {
    for (int grid = 0; grid < 9; grid++) {
      menu.getInputGridSlots().get(grid).set(new ItemStack(Items.STONE, grid + 1));
    }
    menu.getResultSlot().set(new ItemStack(Items.DIAMOND, 3));
    menu.setCarried(new ItemStack(Items.DIRT, 7));
    ServerPlayer serverPlayer = mock(ServerPlayer.class);
    ServerLevel serverLevel = mock(ServerLevel.class);
    when(serverPlayer.level()).thenReturn(serverLevel);
    when(serverPlayer.getInventory()).thenReturn(inventory);

    menu.removed(serverPlayer);

    assertTrue(menu.getCraftSlots().isEmpty());
    assertTrue(menu.getCarried().isEmpty());
    assertTrue(menu.getResultSlot().getItem().isEmpty());
    assertEquals(45, countStored(Items.STONE));
    assertEquals(7, countStored(Items.DIRT));
    assertEquals(0, countStored(Items.DIAMOND), "uncrafted result must not be awarded");
    menu.removed(serverPlayer);
    assertEquals(45, countStored(Items.STONE));
    assertEquals(7, countStored(Items.DIRT));
  }

  private RecipeHolder<CraftingRecipe> threeByThreeRecipe(List<Item> items) {
    ShapedRecipe recipe = mock(ShapedRecipe.class);
    when(recipe.getWidth()).thenReturn(3);
    when(recipe.getHeight()).thenReturn(3);
    when(recipe.placementInfo()).thenReturn(PlacementInfo.create(items.stream().map(Ingredient::of).toList()));
    return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE,
        Identifier.fromNamespaceAndPath("invcrafting", "compatibility_test")), recipe);
  }

  private ServerGamePacketListenerImpl creativePacketHandler() throws ReflectiveOperationException {
    ServerPlayer serverPlayer = mock(ServerPlayer.class);
    ServerLevel serverLevel = mock(ServerLevel.class);
    MinecraftServer server = mock(MinecraftServer.class);
    PacketProcessor processor = mock(PacketProcessor.class);
    when(serverPlayer.level()).thenReturn(serverLevel);
    when(serverPlayer.getInventory()).thenReturn(inventory);
    when(serverPlayer.hasInfiniteMaterials()).thenReturn(true);
    when(serverLevel.enabledFeatures()).thenReturn(FeatureFlags.DEFAULT_FLAGS);
    when(serverLevel.getServer()).thenReturn(server);
    when(server.packetProcessor()).thenReturn(processor);
    when(processor.isSameThread()).thenReturn(true);
    Field inventoryMenu = Player.class.getDeclaredField("inventoryMenu");
    inventoryMenu.setAccessible(true);
    inventoryMenu.set(serverPlayer, menu);
    ServerGamePacketListenerImpl handler = mock(ServerGamePacketListenerImpl.class);
    handler.player = serverPlayer;
    doCallRealMethod().when(handler).handleSetCreativeModeSlot(any());
    return handler;
  }

  private int privateListSize(String name) throws ReflectiveOperationException {
    Field field = AbstractContainerMenu.class.getDeclaredField(name);
    field.setAccessible(true);
    return ((List<?>) field.get(menu)).size();
  }

  private int countStored(Item item) {
    return inventory.getNonEquipmentItems().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
  }

  private static final class SnapshotRemoteSlot implements RemoteSlot {
    private ItemStack expected = ItemStack.EMPTY;

    @Override
    public void force(ItemStack stack) {
      expected = stack.copy();
    }

    @Override
    public void receive(HashedStack stack) {
      throw new AssertionError("The test synchroniser only receives server snapshots");
    }

    @Override
    public boolean matches(ItemStack stack) {
      return ItemStack.matches(expected, stack);
    }
  }
}
