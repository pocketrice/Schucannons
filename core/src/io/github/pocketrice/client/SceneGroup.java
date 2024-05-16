package io.github.pocketrice.client;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SceneGroup extends ModelGroup { // Model group with some add'l helper functionality for compat with gdx-gltf scenemanager
    List<Scene> subscenes;

    public SceneGroup() {
        subscenes = new ArrayList<>();
    }
    public SceneGroup(Model... models) {
        super(models);
        subscenes = new ArrayList<>();
        subscenes.addAll(Arrays.stream(models).map(GameScene::extractScene).toList());
    }

    public SceneGroup(Model[] ms, ModelMeta[] mms) {
        super(ms, mms);
        subscenes = new ArrayList<>();
        subscenes.addAll(Arrays.stream(ms).map(GameScene::extractScene).toList());
    }

    public SceneGroup(ModelInstance[] mis, ModelMeta[] mms) {
        super(mis, mms);
        subscenes = new ArrayList<>();
        subscenes.addAll(Arrays.stream(mis).map(GameScene::extractScene).toList());
    }

    public SceneGroup(ModelGroup other) {
        this();
        for (int i = 0; i < other.submodels.size(); i++) {
            this.submodels.put(other.submodels.keyAt(i), other.submodels.valueAt(i));
            this.submetas.add(other.submetas.get(i));
        }
        this.disposeSet = other.disposeSet;
        this.groupName = other.groupName;
        this.isOffsetApplied = other.isOffsetApplied;
        this.isInterp = other.isInterp;

        subscenes.addAll(submodels.values().stream().map(GameScene::extractScene).toList());
    }

    @Override
    public void addSubmodel(String modelName, ModelInstance modelInst, Vector3 pOff, Quaternion rOff) {
        super.addSubmodel(modelName, modelInst, pOff, rOff);
        subscenes.add(GameScene.extractScene(modelInst));
    }

    public void addScene(String sceneName, SceneAsset sa, Vector3 pOff, Quaternion rOff) {
        Scene sc = new Scene(sa.scene);
        super.addSubmodel(sceneName, sc.modelInstance, pOff, rOff);
        subscenes.add(sc);
    }

    @Override
    public void render(ModelBatch mb){
        System.err.println("SceneGroup cannot be rendered by-batch! Render canceled.");
    }
//
//    @Override
//    public void subrotate(int index, Quaternion rot) {
//        Quaternion oldRot = subscenes.get(index).modelInstance.transform.getRotation(new Quaternion());
//
//        submetas.get(index).interpRot()
//                .from((isInterp) ? oldRot : rot)
//                .to(rot)
//                .type(EasingFunction.EASE_IN_OUT_SINE, 0.1);
//    }
//
//    @Override
//    public void setSubrot(int index, Quaternion rot) {
//        Quaternion fixedRot = new Quaternion().mul(submetas.get(index).rotOffset()).mul(rot);
//        Quaternion oldRot = subscenes.get(index).modelInstance.transform.getRotation(new Quaternion());
//
//        submetas.get(index).interpRot()
//                .from((isInterp) ? oldRot : fixedRot)
//                .to(fixedRot)
//                .type(EasingFunction.EASE_IN_OUT_SINE, 0.1);
//    }
//
//    @Override
//    public void subtranslate(int index, Vector3 pos) {
//        Vector3 oldPos = subscenes.get(index).modelInstance.transform.getTranslation(Vector3.Zero);
//        submetas.get(index).interpPos()
//                .from((isInterp) ? oldPos : pos)
//                .to(pos)
//                .type(EasingFunction.EASE_IN_OUT_SINE, 0.1);
//    }
//
//    @Override
//    public void setSubpos(int index, Vector3 pos) {
//        Vector3 fixedPos = new Vector3().add(pos).add(submetas.get(index).posOffset());
//        Vector3 oldPos = subscenes.get(index).modelInstance.transform.getTranslation(Vector3.Zero);
//
//        submetas.get(index).interpPos()
//                .from((isInterp) ? oldPos : fixedPos)
//                .to(fixedPos)
//                .type(EasingFunction.EASE_IN_OUT_SINE, 0.04);
//    }
//
//    @Override
//    public void subscl(int index, float scalar) {
//        ModelInstance sm = subscenes.get(index).modelInstance;
//        sm.transform.scl(scalar);
//
//        Vector3 pos = sm.transform.getTranslation(new Vector3());
//        float deltaSclX = pos.x * scalar - pos.x;
//        float deltaSclY = pos.y * scalar - pos.y;
//        float deltaSclZ = pos.z * scalar - pos.z;
//
//        sm.transform.translate(deltaSclX, deltaSclY, deltaSclZ);
//    }
//
//    @Override
//    public void func(TriConsumer<ModelInstance, Vector3, Quaternion> func) {
//        for (int i = 0; i < submodels.size(); i++) {
//            func.accept(subscenes.get(i).modelInstance, submetas.get(i).posOffset(), submetas.get(i).rotOffset());
//        }
//    }

    public SceneGroup cpy() {
        return new SceneGroup(super.cpy());
    }
}
