package com.mikuliku.touhoulittlemaidgtnh.client.model;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelMaidJiuHu extends ModelBiped {
    private final ModelRenderer hairBack,hairLeft,hairRight,hairLeftTip,hairRightTip;
    private final ModelRenderer leftBang,rightBang,leftFoxEar,rightFoxEar,maidCap,capRibbon;
    private final ModelRenderer skirtUpper,skirtLower,apron,apronSkirt,apronRibbon;
    private final ModelRenderer tailBase,tailMid,tailTip;

    public ModelMaidJiuHu() {
        super(0F,0F,128,128);

        bipedHead=new ModelRenderer(this,0,0);
        bipedHead.addBox(-4F,-8F,-4F,8,8,8,0F);

        hairBack=new ModelRenderer(this,32,0);
        hairBack.addBox(-5F,-7F,2F,10,14,3,0.25F);
        hairLeft=new ModelRenderer(this,32,20);
        hairLeft.addBox(3F,-5F,-1.5F,3,13,3,0.2F);
        hairRight=new ModelRenderer(this,32,20);
        hairRight.mirror=true;
        hairRight.addBox(-6F,-5F,-1.5F,3,13,3,0.2F);
        hairLeftTip=new ModelRenderer(this,32,36);
        hairLeftTip.addBox(3F,5F,0F,3,6,3,0.15F);
        hairLeftTip.rotateAngleZ=-0.08F;
        hairRightTip=new ModelRenderer(this,32,36);
        hairRightTip.mirror=true;
        hairRightTip.addBox(-6F,5F,0F,3,6,3,0.15F);
        hairRightTip.rotateAngleZ=0.08F;

        leftBang=new ModelRenderer(this,0,20);
        leftBang.addBox(2.5F,-6F,-4.2F,2,6,2,0.12F);
        leftBang.rotateAngleZ=-0.08F;
        rightBang=new ModelRenderer(this,0,20);
        rightBang.mirror=true;
        rightBang.addBox(-4.5F,-6F,-4.2F,2,6,2,0.12F);
        rightBang.rotateAngleZ=0.08F;

        leftFoxEar=new ModelRenderer(this,0,28);
        leftFoxEar.addBox(1F,-12F,-2.5F,4,5,3,0F);
        leftFoxEar.rotateAngleZ=-0.18F;
        rightFoxEar=new ModelRenderer(this,0,28);
        rightFoxEar.mirror=true;
        rightFoxEar.addBox(-5F,-12F,-2.5F,4,5,3,0F);
        rightFoxEar.rotateAngleZ=0.18F;

        maidCap=new ModelRenderer(this,0,36);
        maidCap.addBox(-5F,-9F,-4.5F,10,2,9,0.15F);
        capRibbon=new ModelRenderer(this,24,36);
        capRibbon.addBox(-3F,-8.7F,-5F,6,2,1,0.1F);

        bipedBody=new ModelRenderer(this,16,48);
        bipedBody.addBox(-4F,0F,-2F,8,11,4,0.15F);

        skirtUpper=new ModelRenderer(this,0,60);
        skirtUpper.addBox(-5F,8F,-3F,10,5,6,0.18F);
        skirtLower=new ModelRenderer(this,0,72);
        skirtLower.addBox(-5.8F,12F,-3.4F,12,5,7,0.2F);
        apron=new ModelRenderer(this,24,60);
        apron.addBox(-3F,2.5F,-2.25F,6,7,1,0.05F);
        apronSkirt=new ModelRenderer(this,24,72);
        apronSkirt.addBox(-4.6F,9F,-3.65F,9,6,1,0.05F);
        apronRibbon=new ModelRenderer(this,40,60);
        apronRibbon.addBox(-3F,8F,-2.55F,6,1,1,0.05F);

        bipedRightArm=new ModelRenderer(this,40,48);
        bipedRightArm.addBox(-3F,-2F,-2F,4,12,4,0.15F);
        bipedRightArm.setRotationPoint(-5F,2F,0F);
        bipedLeftArm=new ModelRenderer(this,40,48);
        bipedLeftArm.mirror=true;
        bipedLeftArm.addBox(-1F,-2F,-2F,4,12,4,0.15F);
        bipedLeftArm.setRotationPoint(5F,2F,0F);

        bipedRightLeg=new ModelRenderer(this,16,84);
        bipedRightLeg.addBox(-2F,0F,-2F,4,8,4,0.05F);
        bipedRightLeg.setRotationPoint(-2F,12F,0F);
        bipedLeftLeg=new ModelRenderer(this,16,84);
        bipedLeftLeg.mirror=true;
        bipedLeftLeg.addBox(-2F,0F,-2F,4,8,4,0.05F);
        bipedLeftLeg.setRotationPoint(2F,12F,0F);

        bipedHeadwear=new ModelRenderer(this,0,0);

        tailBase=new ModelRenderer(this,48,52);
        tailBase.addBox(-4F,-2F,0F,8,8,7,0.3F);
        tailBase.setRotationPoint(0F,8F,2.5F);
        tailBase.rotateAngleX=-0.25F;
        tailMid=new ModelRenderer(this,48,68);
        tailMid.addBox(-3.5F,-3F,0F,7,7,8,0.35F);
        tailMid.setRotationPoint(0F,5F,7F);
        tailMid.rotateAngleX=-0.65F;
        tailMid.rotateAngleY=0.15F;
        tailTip=new ModelRenderer(this,48,84);
        tailTip.addBox(-3.5F,-3.5F,0F,7,7,7,0.4F);
        tailTip.setRotationPoint(0F,2F,13F);
        tailTip.rotateAngleX=-0.95F;
        tailTip.rotateAngleY=-0.18F;
    }

    @Override public void render(Entity e,float ls,float lsa,float age,float yaw,float pitch,float scale){
        setRotationAngles(ls,lsa,age,yaw,pitch,scale,e);
        tailBase.render(scale); tailMid.render(scale); tailTip.render(scale);
        bipedHead.render(scale); hairBack.render(scale); hairLeft.render(scale); hairRight.render(scale);
        hairLeftTip.render(scale); hairRightTip.render(scale); leftBang.render(scale); rightBang.render(scale);
        leftFoxEar.render(scale); rightFoxEar.render(scale); maidCap.render(scale); capRibbon.render(scale);
        bipedBody.render(scale); skirtUpper.render(scale); skirtLower.render(scale); apron.render(scale);
        apronSkirt.render(scale); apronRibbon.render(scale);
        bipedRightArm.render(scale); bipedLeftArm.render(scale); bipedRightLeg.render(scale); bipedLeftLeg.render(scale);
    }

    @Override public void setRotationAngles(float ls,float lsa,float age,float yaw,float pitch,float scale,Entity e){
        super.setRotationAngles(ls,lsa,age,yaw,pitch,scale,e);
        leftFoxEar.rotateAngleY=bipedHead.rotateAngleY; rightFoxEar.rotateAngleY=bipedHead.rotateAngleY;
        leftFoxEar.rotateAngleX=bipedHead.rotateAngleX-0.08F; rightFoxEar.rotateAngleX=bipedHead.rotateAngleX-0.08F;
        hairBack.rotateAngleY=bipedHead.rotateAngleY*0.55F; hairBack.rotateAngleX=bipedHead.rotateAngleX*0.25F;
        hairLeft.rotateAngleY=bipedHead.rotateAngleY*0.35F; hairRight.rotateAngleY=bipedHead.rotateAngleY*0.35F;
        hairLeftTip.rotateAngleY=bipedHead.rotateAngleY*0.25F; hairRightTip.rotateAngleY=bipedHead.rotateAngleY*0.25F;
        leftBang.rotateAngleY=bipedHead.rotateAngleY; rightBang.rotateAngleY=bipedHead.rotateAngleY;
        maidCap.rotateAngleY=bipedHead.rotateAngleY; capRibbon.rotateAngleY=bipedHead.rotateAngleY;
        float sway=(float)Math.sin(age*0.08F)*0.08F;
        tailBase.rotateAngleY=sway; tailMid.rotateAngleY=0.15F+sway*1.5F; tailTip.rotateAngleY=-0.18F+sway*2F;
        float walk=Math.abs((float)Math.sin(ls*0.55F))*lsa*0.08F;
        tailBase.rotateAngleX=-0.25F-walk; tailMid.rotateAngleX=-0.65F-walk*1.5F; tailTip.rotateAngleX=-0.95F-walk*2F;
    }
}
