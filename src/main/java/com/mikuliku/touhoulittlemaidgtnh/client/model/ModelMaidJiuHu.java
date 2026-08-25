package com.mikuliku.touhoulittlemaidgtnh.client.model;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * 酒狐（Wine Fox）1.7.10 模型。
 *
 * 按官方“大正女仆酒狐”的主要视觉结构重建为 Forge 1.7.10
 * 可直接使用的 ModelRenderer：长金发、狐耳、大正女仆服、
 * 围裙、裙摆以及蓬松狐尾。
 */
public class ModelMaidJiuHu extends ModelBiped {

    private final ModelRenderer hairBack;
    private final ModelRenderer hairLeft;
    private final ModelRenderer hairRight;
    private final ModelRenderer leftFoxEar;
    private final ModelRenderer rightFoxEar;
    private final ModelRenderer maidCap;
    private final ModelRenderer maidSkirt;
    private final ModelRenderer apron;
    private final ModelRenderer apronSkirt;
    private final ModelRenderer tailBase;
    private final ModelRenderer tailMid;
    private final ModelRenderer tailTip;

    public ModelMaidJiuHu() {
        super(0.0F, 0.0F, 128, 128);

        this.bipedHead = new ModelRenderer(this, 0, 0);
        this.bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.0F);
        this.bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.hairBack = new ModelRenderer(this, 32, 0);
        this.hairBack.addBox(-5.0F, -7.0F, 2.0F, 10, 13, 3, 0.25F);
        this.hairBack.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.hairLeft = new ModelRenderer(this, 32, 20);
        this.hairLeft.addBox(3.0F, -5.0F, -1.5F, 3, 12, 3, 0.2F);
        this.hairLeft.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.hairRight = new ModelRenderer(this, 32, 20);
        this.hairRight.mirror = true;
        this.hairRight.addBox(-6.0F, -5.0F, -1.5F, 3, 12, 3, 0.2F);
        this.hairRight.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.leftFoxEar = new ModelRenderer(this, 0, 20);
        this.leftFoxEar.addBox(1.0F, -11.0F, -2.5F, 4, 4, 3, 0.0F);
        this.leftFoxEar.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.leftFoxEar.rotateAngleZ = -0.18F;
        this.leftFoxEar.rotateAngleX = -0.08F;

        this.rightFoxEar = new ModelRenderer(this, 0, 20);
        this.rightFoxEar.mirror = true;
        this.rightFoxEar.addBox(-5.0F, -11.0F, -2.5F, 4, 4, 3, 0.0F);
        this.rightFoxEar.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.rightFoxEar.rotateAngleZ = 0.18F;
        this.rightFoxEar.rotateAngleX = -0.08F;

        this.maidCap = new ModelRenderer(this, 0, 28);
        this.maidCap.addBox(-5.0F, -8.8F, -4.5F, 10, 2, 9, 0.15F);
        this.maidCap.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.bipedBody = new ModelRenderer(this, 16, 36);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 11, 4, 0.15F);
        this.bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.maidSkirt = new ModelRenderer(this, 0, 52);
        this.maidSkirt.addBox(-5.5F, 8.0F, -3.0F, 11, 7, 6, 0.15F);
        this.maidSkirt.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.apronSkirt = new ModelRenderer(this, 0, 66);
        this.apronSkirt.addBox(-4.5F, 9.0F, -3.35F, 9, 6, 1, 0.05F);
        this.apronSkirt.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.apron = new ModelRenderer(this, 24, 66);
        this.apron.addBox(-3.0F, 2.5F, -2.25F, 6, 7, 1, 0.05F);
        this.apron.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.bipedRightArm = new ModelRenderer(this, 40, 36);
        this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.15F);
        this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);

        this.bipedLeftArm = new ModelRenderer(this, 40, 36);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.15F);
        this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);

        this.bipedRightLeg = new ModelRenderer(this, 16, 80);
        this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4, 0.05F);
        this.bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);

        this.bipedLeftLeg = new ModelRenderer(this, 16, 80);
        this.bipedLeftLeg.mirror = true;
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4, 0.05F);
        this.bipedLeftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);

        this.bipedHeadwear = new ModelRenderer(this, 0, 0);
        this.bipedHeadwear.setRotationPoint(0.0F, 0.0F, 0.0F);

        this.tailBase = new ModelRenderer(this, 48, 52);
        this.tailBase.addBox(-4.0F, -2.0F, 0.0F, 8, 8, 7, 0.3F);
        this.tailBase.setRotationPoint(0.0F, 8.0F, 2.5F);
        this.tailBase.rotateAngleX = -0.25F;

        this.tailMid = new ModelRenderer(this, 48, 68);
        this.tailMid.addBox(-3.5F, -3.0F, 0.0F, 7, 7, 7, 0.35F);
        this.tailMid.setRotationPoint(0.0F, 5.0F, 7.5F);
        this.tailMid.rotateAngleX = -0.65F;
        this.tailMid.rotateAngleY = 0.15F;

        this.tailTip = new ModelRenderer(this, 48, 84);
        this.tailTip.addBox(-3.0F, -3.0F, 0.0F, 6, 6, 6, 0.4F);
        this.tailTip.setRotationPoint(0.0F, 2.0F, 12.5F);
        this.tailTip.rotateAngleX = -0.95F;
        this.tailTip.rotateAngleY = -0.18F;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale, entity);

        this.tailBase.render(scale);
        this.tailMid.render(scale);
        this.tailTip.render(scale);

        this.bipedHead.render(scale);
        this.hairBack.render(scale);
        this.hairLeft.render(scale);
        this.hairRight.render(scale);
        this.leftFoxEar.render(scale);
        this.rightFoxEar.render(scale);
        this.maidCap.render(scale);

        this.bipedBody.render(scale);
        this.maidSkirt.render(scale);
        this.apronSkirt.render(scale);
        this.apron.render(scale);

        this.bipedRightArm.render(scale);
        this.bipedLeftArm.render(scale);
        this.bipedRightLeg.render(scale);
        this.bipedLeftLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount,
                                  float ageInTicks, float netHeadYaw,
                                  float headPitch, float scaleFactor,
                                  Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scaleFactor, entityIn);

        this.leftFoxEar.rotateAngleY = this.bipedHead.rotateAngleY;
        this.leftFoxEar.rotateAngleX = this.bipedHead.rotateAngleX - 0.08F;
        this.rightFoxEar.rotateAngleY = this.bipedHead.rotateAngleY;
        this.rightFoxEar.rotateAngleX = this.bipedHead.rotateAngleX - 0.08F;

        this.hairBack.rotateAngleY = this.bipedHead.rotateAngleY * 0.55F;
        this.hairBack.rotateAngleX = this.bipedHead.rotateAngleX * 0.25F;
        this.hairLeft.rotateAngleY = this.bipedHead.rotateAngleY * 0.35F;
        this.hairRight.rotateAngleY = this.bipedHead.rotateAngleY * 0.35F;

        float sway = (float) Math.sin(ageInTicks * 0.08F) * 0.08F;
        this.tailBase.rotateAngleY = sway;
        this.tailMid.rotateAngleY = 0.15F + sway * 1.5F;
        this.tailTip.rotateAngleY = -0.18F + sway * 2.0F;
    }
}
