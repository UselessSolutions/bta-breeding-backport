package useless.btabreeding.mixin;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.MobPathfinder;
import net.minecraft.core.entity.animal.Creature;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import useless.btabreeding.BtaBreeding;
import useless.btabreeding.IBreeding;

import java.util.List;

@Mixin(value = MobAnimal.class, remap = false)
public abstract class MobAnimalMixin extends MobPathfinder implements Creature, IBreeding {
	@Shadow
	public abstract boolean isFavouriteItem(ItemStack itemStack);

	public MobAnimalMixin(final World world) {
		super(world);
	}

	@Unique
	public int breedingTimer = 0;
	@Unique
	public int fedTimer = 0;
	@Unique
	public int childhoodTimer = 0;
	@Unique
	public boolean isPersistent = false;
	@Unique
	public Entity passiveTarget = null;

	@Override
	public int btabreeding$getBreedingTimer() {
		return this.breedingTimer;
	}

	@Override
	public int btabreeding$getFedTimer() {
		return this.fedTimer;
	}

	@Override
	public int btabreeding$getChildTimer() {
		return this.childhoodTimer;
	}

	@Override
	public void btabreeding$setBreedingTimer(final int value) {
		this.breedingTimer = value;
	}

	@Override
	public void btabreeding$setFedTimer(final int value) {
		this.fedTimer = value;
	}

	@Override
	public void btabreeding$setChildTimer(final int value) {
		this.childhoodTimer = value;
	}

	@Override
	public boolean btabreeding$isBreedable() {
		return this.breedingTimer <= 0 && !btabreeding$isBaby();
	}

	@Override
	public boolean btabreeding$isFed() {
		return this.fedTimer > 0;
	}

	@Override
	public boolean btabreeding$isFoodItem(final ItemStack stack) {
		return isFavouriteItem(stack);
	}

	@Override
	public void btabreeding$spawnBaby(final IBreeding partner) {
		if (!this.world.isClientSide) {
			// Spawn entity
			final MobAnimal entity = (MobAnimal) BtaBreeding.createEntity(this.getClass(), this.world);
			entity.moveTo(this.x, this.y, this.z, 0, 0.0f);
			entity.spawnInit();

			((IBreeding) entity).btabreeding$setChildTimer(20 * 60 * 5);

			this.world.entityJoinedWorld(entity);
			this.btabreeding$setFedTimer(0);
			partner.btabreeding$setFedTimer(0);
			this.btabreeding$setBreedingTimer(20*100);
			partner.btabreeding$setBreedingTimer(20*100);
			this.btabreeding$setPassiveTarget(null);
			partner.btabreeding$setPassiveTarget(null);
		}
	}

	@Override
	public boolean btabreeding$isBaby() {
		return btabreeding$getChildTimer() > 0;
	}
	@Override
	public void btabreeding$setPassiveTarget(final Entity entity){
		this.passiveTarget = entity;
		if (entity == null){
			this.pathToEntity = null;
		}
	}
	@Override
	public Entity btabreeding$getPassiveTarget() {
		return this.passiveTarget;
	}

	@Override
	public boolean interact(final Player entityplayer) {
		final ItemStack item = entityplayer.inventory.getCurrentItem();
		final boolean flag = super.interact(entityplayer);
		if (item != null && btabreeding$isFoodItem(item) && (btabreeding$isBreedable() || btabreeding$isBaby()) && item.consumeItem(entityplayer)){
			if (this.btabreeding$isBaby()){
				this.btabreeding$setChildTimer((int) (btabreeding$getChildTimer() * 0.75f));
				final double d = this.random.nextGaussian() * 0.02;
				final double d1 = this.random.nextGaussian() * 0.02;
				final double d2 = this.random.nextGaussian() * 0.02;
				this.world.spawnParticle(
					"soulflame",
					this.x + (double)(this.random.nextFloat() * this.bbWidth * 2.0F) - (double)this.bbWidth,
					this.y + 0.5 + (double)(this.random.nextFloat() * this.bbHeight),
					this.z + (double)(this.random.nextFloat() * this.bbWidth * 2.0F) - (double)this.bbWidth,
					d,
					d1,
					d2,
					0
				);
			} else {
				this.btabreeding$setFedTimer(20 * 15);
				this.isPersistent = true;
			}
			return true;
		}
		return flag;
	}

	@Override
	public void onLivingUpdate() {
		if (this.breedingTimer > 0){
			this.breedingTimer--;
		}
		if (btabreeding$getChildTimer() > 0){
			this.isPersistent = true;
			btabreeding$setChildTimer(btabreeding$getChildTimer()-1);
		}
		if (btabreeding$isFed()){
			this.fedTimer--;
		}
		List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.bb.expand(0.2F, 0.0, 0.2F).move(-0.1F, -0.1F, -0.1F));
		if (list != null && !list.isEmpty() && !isMovementCeased()) {
            for (final Entity entity : list) {
                if (entity instanceof IBreeding &&
					entity.getClass().isInstance(this) &&
					this.btabreeding$isFed() &&
					((IBreeding) entity).btabreeding$isFed() &&
					this.btabreeding$isBreedable() &&
					((IBreeding) entity).btabreeding$isBreedable())
				{
                    this.btabreeding$spawnBaby((IBreeding) entity);
					break;
                }
            }
		}

		if (this.tickCount % 40 == 0 && !isMovementCeased()){
			list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.bb.expand(10F, 10F, 10F).move(-5F, -5F, -5F));
			if (btabreeding$isBaby() && btabreeding$getPassiveTarget() == null){
				for (final Entity entity : list) {
					if (entity instanceof IBreeding &&
						entity.getClass().isInstance(this) &&
					!((IBreeding) entity).btabreeding$isBaby()) {
						this.btabreeding$setPassiveTarget(entity);
						break;
					}
				}
			}
			else if (btabreeding$isFed()){
				this.btabreeding$setPassiveTarget(null);
				for (final Entity entity : list) {
					if (entity instanceof IBreeding &&
						entity.getClass().isInstance(this) &&
						this.btabreeding$isFed() &&
						((IBreeding) entity).btabreeding$isFed() &&
						this.btabreeding$isBreedable() &&
						((IBreeding) entity).btabreeding$isBreedable()) {
						this.btabreeding$setPassiveTarget(entity);
						break;
					}
				}

			} else if (btabreeding$isBreedable()) {
				this.btabreeding$setPassiveTarget(null);
				for (final Entity entity : list) {
					if (entity instanceof Player && btabreeding$isFoodItem(((Player) entity).getHeldItem())) {
						this.btabreeding$setPassiveTarget(entity);
						break;
					}
				}
			}

		}

		super.onLivingUpdate();

		if (btabreeding$isFed() && this.tickCount % 4 == 0){
			final double d = this.random.nextGaussian() * 0.02;
			final double d1 = this.random.nextGaussian() * 0.02;
			final double d2 = this.random.nextGaussian() * 0.02;
			this.world.spawnParticle(
					"heart",
					this.x + (double)(this.random.nextFloat() * this.bbWidth * 2.0F) - (double)this.bbWidth,
					this.y + 0.5 + (double)(this.random.nextFloat() * this.bbHeight),
					this.z + (double)(this.random.nextFloat() * this.bbWidth * 2.0F) - (double)this.bbWidth,
					d,
					d1,
					d2,
				0
				);
		}
	}
	/**
	 * @author Useless
	 * @reason Replacing vanilla follow logic with new logic
	 */
	@Overwrite
	public void updateAI() {
		if (passiveTarget == null && getCurrentTarget() == null){
			pathToEntity = null;
		}
		if (passiveTarget != null){
			if (passiveTarget instanceof Player && distanceToSqr(passiveTarget) < 9){
				pathToEntity = null;
			} else {
				pathToEntity = world.getPathToEntity(this, passiveTarget, 20);
			}
		}

		super.updateAI();
	}
	@Inject(method = "addAdditionalSaveData(Lcom/mojang/nbt/tags/CompoundTag;)V", at = @At("TAIL"))
	public void saveData(final CompoundTag tag, final CallbackInfo ci){
		tag.putInt("breeding$breedtime", this.breedingTimer);
		tag.putInt("breeding$fedtime", this.fedTimer);
		tag.putInt("breeding$childtime", this.childhoodTimer);
		tag.putBoolean("breeding$persistent", this.isPersistent);
	}

	@Inject(method = "readAdditionalSaveData(Lcom/mojang/nbt/tags/CompoundTag;)V", at = @At("TAIL"))
	public void loadData(final CompoundTag tag, final CallbackInfo ci){
		this.breedingTimer = tag.getInteger("breeding$breedtime");
		this.fedTimer =	tag.getInteger("breeding$fedtime");
		this.childhoodTimer = tag.getInteger("breeding$childtime");
		this.isPersistent = tag.getBoolean("breeding$persistent");
	}
	@Override
	public boolean canDespawn(){
		return super.canDespawn() && !this.isPersistent;
	}
}
