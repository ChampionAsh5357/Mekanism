package mekanism.client.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import mekanism.api.gear.IModuleHelper;
import mekanism.client.render.armor.MekaSuitArmor.ModuleOBJModelData;
import mekanism.client.render.transmitter.RenderTransmitterBase;
import mekanism.common.Mekanism;
import mekanism.common.registries.MekanismRobitSkins.SkinLookup;
import mekanism.common.tile.qio.TileEntityQIODriveArray.DriveStatus;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent.BakingCompleted;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MekanismModelCache extends BaseModelCache {

    public static final MekanismModelCache INSTANCE = new MekanismModelCache();
    private final Set<Runnable> callbacks = new HashSet<>();

    public final OBJModelData MEKASUIT = registerOBJ("models/entity/mekasuit.obj");
    public final OBJModelData MEKATOOL_LEFT_HAND = registerOBJ("models/entity/mekatool_left.obj");
    public final OBJModelData MEKATOOL_RIGHT_HAND = registerOBJ("models/entity/mekatool_right.obj");
    private final Set<ModuleOBJModelData> mekaSuitModules = new HashSet<>();
    public final Set<ModuleOBJModelData> MEKASUIT_MODULES = Collections.unmodifiableSet(mekaSuitModules);

    public final OBJModelData TRANSMITTER_CONTENTS = register(RenderTransmitterBase.MODEL_LOCATION, rl -> new OBJModelData(rl) {
        @Override
        protected boolean useDiffuseLighting() {
            return false;
        }
    });
    public final JSONModelData LIQUIFIER_BLADE = registerJSON("block/liquifier_blade");
    public final JSONModelData VIBRATOR_SHAFT = registerJSON("block/vibrator_shaft");
    public final JSONModelData PIGMENT_MIXER_SHAFT = registerJSON("block/pigment_mixer_shaft");
    public final JSONModelData[] QIO_DRIVES = new JSONModelData[DriveStatus.STATUSES.length];
    private final Map<ResourceLocation, JSONModelData> CUSTOM_ROBIT_MODELS = new HashMap<>();
    private final Map<ResourceLocation, JSONModelData> ROBIT_SKINS = new HashMap<>();
    private BakedModel BASE_ROBIT;

    private MekanismModelCache() {
        super(Mekanism.MODID);
        for (DriveStatus status : DriveStatus.STATUSES) {
            if (status != DriveStatus.NONE) {
                QIO_DRIVES[status.ordinal()] = registerJSON(status.getModel());
            }
        }
    }

    @Override
    public void onBake(BakingCompleted evt) {
        super.onBake(evt);
        callbacks.forEach(Runnable::run);
        BASE_ROBIT = getBakedModel(evt, new ModelResourceLocation(Mekanism.rl("robit"), "inventory"));
        //Clear old robit skin caches
        //Note: We don't clear the cached models as the old JSONModelDatas should be able to properly handle reloading,
        // and we only clear the skin cache in case the skin no longer has a custom model (even though this is highly unlikely)
        ROBIT_SKINS.clear();
    }

    public void reloadCallback(Runnable callback) {
        callbacks.add(callback);
    }

    @Nullable
    public BakedModel getRobitSkin(@NotNull SkinLookup skinLookup) {
        JSONModelData data = ROBIT_SKINS.computeIfAbsent(skinLookup.location(), skinName -> {
            ResourceLocation customModel = skinLookup.skin().customModel();
            if (customModel != null) {
                //If multiple skins make use of the same custom model, have them all point at the same model data object
                return CUSTOM_ROBIT_MODELS.computeIfAbsent(customModel, this::registerJSONAndBake);
            }
            return null;
        });
        return data == null ? BASE_ROBIT : data.getBakedModel();
    }

    /**
     * Call via {@link IModuleHelper#addMekaSuitModuleModels(ResourceLocation)}.
     */
    public ModuleOBJModelData registerMekaSuitModuleModel(ResourceLocation rl) {
        ModuleOBJModelData data = register(rl, ModuleOBJModelData::new);
        mekaSuitModules.add(data);
        return data;
    }
}
