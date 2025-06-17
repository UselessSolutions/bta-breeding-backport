package useless.btabreeding.mixin;

import net.minecraft.core.entity.MobPathfinder;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.animal.MobSheep;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import useless.btabreeding.BtaBreeding;
import useless.btabreeding.IBreeding;

@Mixin(value = MobSheep.class, remap = false)
public abstract class MobSheepMixin extends MobAnimalMixin {
	@Shadow
	public abstract DyeColor getFleeceColor();

	public MobSheepMixin(final World world) {
		super(world);
	}
	@Override
	public void btabreeding$spawnBaby(final IBreeding partner) {
		if (!this.world.isClientSide) {
			// Spawn entity
			final MobAnimal entity = (MobAnimal) BtaBreeding.createEntity(this.getClass(), this.world);
			entity.moveTo(this.x, this.y, this.z, 0, 0.0f);
			entity.spawnInit();

			((MobSheep)entity).setFleeceColor(this.random.nextInt(2) == 0 ? this.getFleeceColor() : ((MobSheep)partner).getFleeceColor());

			((IBreeding) entity).btabreeding$setChildTimer(20 * 60 * 5);

			this.world.entityJoinedWorld(entity);
			this.btabreeding$setFedTimer(0);
			partner.btabreeding$setFedTimer(0);
			this.btabreeding$setBreedingTimer(20*100);
			partner.btabreeding$setBreedingTimer(20*100);
			this.setTarget(null);
			if (partner instanceof MobPathfinder){
				((MobPathfinder) partner).setTarget(null);
			}
		}
	}
}
