package com.mikuliku.touhoulittlemaidgtnh.client.model;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * 酒狐的 1.7.10 女仆风格基础模型。
 *
 * 这是一个兼容 Forge 1.7.10 的纯 Java 模型，不依赖现代版模型系统。
 * 后续可以在此基础上继续替换为完整车万女仆模型。
 */
public class ModelMaidJiuHu extends ModelBiped {

    private final ModelRenderer maidSkirt;
    private final ModelRenderer apron;
    private final ModelRenderer hairBack;
    private final ModelRenderer maidHeadband;

    public ModelMaidJiuHu() {
        super(0.0F, 0.0F, 64, 64);

        // 头部
        this.bipedHead = new ModelRenderer(this, 0, 0);
        this.bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.0F);
        this.bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);

        // 后发
        this.hairBack = new ModelRenderer(this, 32, 0);
        this.hairBack.addBox(-4.5F, -7.0F, 2.5F, 9, 10, 2, 0.0F);
        this.hairBack.setRotationPoint(0.0F, 0.0F, 0.0F);

        // 身体
        this.bipedBody = new ModelRenderer(this, 16, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.0F);
        this.bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);

        // 女仆裙
        this.maidSkirt = new ModelRenderer(this, 0, 32);
        this.maidSkirt.addBox(-5.5F, 9.0F, -3.0F, 11, 7, 6, 0.0F);
        this.maidSkirt.setRotationPoint(0.0F, 0.0F, 0.0F);

        // 围裙
        this.apron = new ModelRenderer(this, 34, 32);
        this.apron.addBox(-3.0F, 3.0F, -2.15F, 6, 7, 1, 0.0F);
        this.apron.setRotationPoint(0.0F, 0.0F, 0.0F);

        // 发箍
        this.maidHeadband = new ModelRenderer(this, 0, 48);
        this.maidHeadband.addBox(-5.0F, -5.0F, -4.5F, 10, 2, 1, 0.0F);
        this.maidHeadband.setRotationPoint(0.0F, 0.0F, 0.0F);

        // 手臂
        this.bipedRightArm = new ModelRenderer(this, 40, 16);
        this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.0F);
        this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);

        this.bipedLeftArm = new ModelRenderer(this, 40, 16);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.0F);
        this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);

        // 腿
        this.bipedRightLeg = new ModelRenderer(this, 16, 32);
        this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4, 0.0F);
        this.bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);

        this.bipedLeftLeg = new ModelRenderer(this, 16, 32);
        this.bipedLeftLeg.mirror = true;
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4, 0.0F);
        this.bipedLeftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);

        this.bipedHeadwear = new ModelRenderer(this, 0, 0);
        this.bipedHeadwear.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.bipedHeadwear.addBox(-4.5F, -8.5F, -4.5F, 9, 9, 9, 0.5F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch,
                       float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale, entity);

        this.bipedHead.render(scale);
        this.hairBack.render(scale);
        this.maidHeadband.render(scale);
        this.bipedBody.render(scale);
        this.maidSkirt.render(scale);
        this.apron.render(scale);
        this.bipedRightArm.render(scale);
        this.bipedLeftArm.render(scale);
        this.bipedRightLeg.render(scale);
        this.bipedLeftLeg.render(scale);
    }
}
