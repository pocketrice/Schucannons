package io.github.pocketrice.client;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.EvictingMap;
import lombok.Setter;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelGroup implements Disposable {
    EvictingMap<String, ModelInstance> submodels;
    List<ModelMeta> submetas;
    Set<Disposable> disposeSet;
    @Setter
    String groupName;
    @Setter
    boolean isOffsetApplied, isInterp;

    public ModelGroup() {
        submodels = new EvictingMap<>(999); // note ~ using EvictingMap for ability to get @ indices... limit is not intentional.
        submetas = new ArrayList<>();
        disposeSet = new HashSet<>();
        isOffsetApplied = false;
        isInterp = true;
    }

    public ModelGroup(Model... models) {
        this(models, generateUArr(new ModelMeta[models.length], ModelMeta.class));
    }

    public ModelGroup(ModelInstance... modelInsts) {
        this(modelInsts, generateUArr(new ModelMeta[modelInsts.length], ModelMeta.class));
    }

    public ModelGroup(Model[] ms, ModelMeta[] mms) {
        this();

        for (int i = 0; i < ms.length; i++) {
            addSubmodel(ms[i], mms[i].posOffset(), mms[i].rotOffset());
        }

        isOffsetApplied = false;
        isInterp = true;

    }

    public ModelGroup(ModelInstance[] mis, ModelMeta[] mms) {
        this();

        for (int i = 0; i < mis.length; i++) {
            addSubmodel(mis[i], mms[i].posOffset(), mms[i].rotOffset());
        }

        isOffsetApplied = false;
        isInterp = true;
    }

    public void addSubmodel(Model model, Vector3 pOff, Quaternion rOff) {
        disposeSet.add(model);
        addSubmodel(new ModelInstance(model), pOff, rOff);
    }

    public void addSubmodel(ModelInstance modelInst, Vector3 pOff, Quaternion rOff) {
        addSubmodel("sm" + submodels.size(), modelInst, pOff, rOff);
    }

    public void addSubmodel(String modelName, ModelInstance modelInst, Vector3 pOff, Quaternion rOff) {
        submodels.put(modelName, modelInst);
        submetas.add(new ModelMeta(pOff, rOff));
    }

    // Should only be called once PRIOR to anything.
    public void applyOffsets() {
        for (int i = 0; i < submodels.size(); i++) {
            submodels.valueAt(i).transform.translate(submetas.get(i).posOffset());
            submodels.valueAt(i).transform.rotate(submetas.get(i).rotOffset());
        }

        isOffsetApplied = true;
    }

    public void update() { // Per-tick updates
        for (int i = 0; i < submodels.size(); i++) {
            ModelMeta submeta = submetas.get(i);
            Matrix4 transform = submodels.valueAt(i).transform;

            Quaternion rot = new Quaternion().mul(submeta.rotOffset()).mul(submeta.interpRot().advance());
            transform.setFromEulerAngles(rot.getYaw(), rot.getPitch(), rot.getRoll());
            transform.setTranslation(new Vector3().add(submeta.posOffset()).add(submeta.interpPos().advance()));
            System.out.println(this);
        }

        System.out.println("\n");
    }

    public void render(ModelBatch mb) {
        for (ModelInstance sm : submodels.values()) {
            mb.render(sm);
        }
    }

    public void rotate(Quaternion rot) { // Additive (+)
        if (!isOffsetApplied)
            System.err.println("Warning: ModelGroup " + groupName + " offsets not applied — may be inaccurate!");
        for (int i = 0; i < submodels.size(); i++) {
            subrotate(i, rot);
        }
    }

    public void subrotate(int index, Quaternion rot) {
        Quaternion oldRot = submodels.valueAt(index).transform.getRotation(new Quaternion());

        submetas.get(index).interpRot()
                .from((isInterp) ? oldRot : rot)
                .to(rot)
                .type(EasingFunction.EASE_IN_OUT_SINE, 0.1);
    }

    public void setRot(Quaternion rot) { // Replacing (=)
        for (int i = 0; i < submodels.size(); i++) {
            setSubrot(i, rot);
        }
    }

    public void setSubrot(int index, Quaternion rot) {
        Quaternion fixedRot = new Quaternion().mul(submetas.get(index).rotOffset()).mul(rot);
        Quaternion oldRot = submodels.valueAt(index).transform.getRotation(new Quaternion());

        submetas.get(index).interpRot()
                .from((isInterp) ? oldRot : fixedRot)
                .to(fixedRot)
                .type(EasingFunction.EASE_IN_OUT_SINE, 0.1);
    }

    public void translate(Vector3 pos) { // Additive (+)
        if (!isOffsetApplied)
            System.err.println("Warning: ModelGroup " + groupName + " offsets not applied — may be inaccurate!");
        for (int i = 0; i < submodels.size(); i++) {
            subtranslate(i, pos);
        }
    }

    public void subtranslate(int index, Vector3 pos) {
        Vector3 oldPos = submodels.valueAt(index).transform.getTranslation(Vector3.Zero);
        submetas.get(index).interpPos()
                .from((isInterp) ? oldPos : pos)
                .to(pos)
                .type(EasingFunction.EASE_IN_OUT_SINE, 0.1);
    }

    public void setPos(Vector3 pos) { // Replacing (=)
        for (int i = 0; i < submodels.size(); i++) {
            setSubpos(i, pos);
        }
    }

    public void setSubpos(int index, Vector3 pos) {
        Vector3 fixedPos = new Vector3().add(pos).add(submetas.get(index).posOffset());
        Vector3 oldPos = submodels.valueAt(index).transform.getTranslation(Vector3.Zero);

        submetas.get(index).interpPos()
                .from((isInterp) ? oldPos : fixedPos)
                .to(fixedPos)
                .type(EasingFunction.EASE_IN_OUT_SINE, 0.04);
    }

    public void scl(float scalar) { // Additive (+)
        if (!isOffsetApplied)
            System.err.println("Warning: ModelGroup " + groupName + " offsets not applied — may be inaccurate!");
        for (int i = 0; i < submodels.size(); i++) {
            subscl(i, scalar);
        }
    }

    public void subscl(int index, float scalar) {
        ModelInstance sm = submodels.valueAt(index);
        sm.transform.scl(scalar);

        Vector3 pos = sm.transform.getTranslation(new Vector3());
        float deltaSclX = pos.x * scalar - pos.x;
        float deltaSclY = pos.y * scalar - pos.y;
        float deltaSclZ = pos.z * scalar - pos.z;

        sm.transform.translate(deltaSclX, deltaSclY, deltaSclZ);
    }

    public void setScl(float scalar) { // Replacing (=)
        for (int i = 0; i < submodels.size(); i++) {
            setSubscl(i, scalar);
        }
    }

    public void setSubscl(int index, float scalar) {
        submodels.valueAt(index).transform.setToScaling(scalar, scalar, scalar); // Reset scale
    }


    public void func(TriConsumer<ModelInstance, Vector3, Quaternion> func) {
        for (int i = 0; i < submodels.size(); i++) {
            func.accept(submodels.valueAt(i), submetas.get(i).posOffset(), submetas.get(i).rotOffset());
        }
    }

    public ModelGroup cpy() {
        ModelGroup copy = new ModelGroup();
        for (int i = 0; i < submodels.size(); i++) {
            copy.addSubmodel(submodels.valueAt(i), submetas.get(i).posOffset(), submetas.get(i).rotOffset());
        }
        copy.setOffsetApplied(isOffsetApplied);

        return copy;
    }

    @Override
    public String toString() {
        return groupName + "{" + submodels.entrySet().stream().map(sm -> sm.getKey() + "[" + sm.getValue().transform.getRotation(new Quaternion()) + "/" + sm.getValue().transform.getTranslation(Vector3.Zero) + "]").collect(Collectors.joining(", ")) + " }";
    }

    // Adapted from LibGDX's own setEulerAnglesRad implementation
    public static Vector3 quatToEuler(Quaternion quat) {
        return new Vector3(quat.getYaw(), quat.getPitch(), quat.getRoll());
    }

    public static Quaternion eulerToQuat(Vector3 euler) {
        return new Quaternion().setEulerAngles(euler.x, euler.y, euler.z);
    }

    public static <T> List<T> generateUList(int len, Class<T> type) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            try {
                list.add(type.getConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return list;
    }

    public static <T> T[] generateUArr(T[] arr, Class<T> type) {
        for (int i = 0; i < arr.length; i++) {
            try {
                arr[i] = type.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return arr;
    }

    @Override
    public void dispose() {
        disposeSet.forEach(Disposable::dispose);
    }
}
