package io.github.pocketrice.client;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import lombok.Setter;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.Arrays;

public class ModelGroup {
    @Setter
    String groupName;
    ModelInstance[] submodels;
    Vector3[] posOffsets;
    Quaternion[] rotOffsets;
    @Setter
    boolean isOffsetApplied;

    public ModelGroup() {
        this(new Model[0], new Vector3[0], new Quaternion[0]);
    }
    public ModelGroup(Model... models) {
        this(models, new Vector3[models.length], new Quaternion[models.length]);
    }

    public ModelGroup(Model[] models, Vector3[] pOff, Quaternion[] rOff) {
        submodels = new ModelInstance[models.length];

        for (int i = 0; i < models.length; i++) {
            submodels[i] = new ModelInstance(models[i]);
        }

        posOffsets = pOff;
        rotOffsets = rOff;

        isOffsetApplied = false;
    }

    public ModelGroup(ModelInstance... modelInsts) {
        this(modelInsts, new Vector3[modelInsts.length], new Quaternion[modelInsts.length]);
    }

    public ModelGroup(ModelInstance[] modelInsts, Vector3[] pOff, Quaternion[] rOff) {
        submodels = modelInsts;
        posOffsets = pOff;
        rotOffsets = rOff;

        isOffsetApplied = false;
    }

    public void addSubmodel(Model model, Vector3 pOff, Quaternion rOff) {
        addSubmodel(new ModelInstance(model), pOff, rOff);
    }

    public void addSubmodel(ModelInstance modelInst, Vector3 pOff, Quaternion rOff) {
        submodels = Arrays.copyOf(submodels, submodels.length + 1);
        posOffsets = Arrays.copyOf(posOffsets, posOffsets.length + 1);
        rotOffsets = Arrays.copyOf(rotOffsets, rotOffsets.length + 1);

        submodels[submodels.length-1] = modelInst;
        posOffsets[posOffsets.length-1] = pOff;
        rotOffsets[rotOffsets.length-1] = rOff;
    }

    // Should only be called once PRIOR to anything.
    public void applyOffsets() {
        for (int i = 0; i < submodels.length; i++) {
            submodels[i].transform.translate(posOffsets[i]);
            submodels[i].transform.rotate(rotOffsets[i]);
        }
        isOffsetApplied = true;
    }

    public void render(ModelBatch mb) {
        for (ModelInstance sm : submodels) {
            mb.render(sm);
        }
    }

    public void rotate(Quaternion rot) {
        if (!isOffsetApplied) System.err.println("Warning: ModelGroup " + groupName + " offsets not applied — may be inaccurate!");
        for (ModelInstance submodel : submodels) {
           submodel.transform.rotate(rot);
        }
    }

    public void translate(Vector3 translation) {
        if (!isOffsetApplied) System.err.println("Warning: ModelGroup " + groupName + " offsets not applied — may be inaccurate!");
        for (ModelInstance submodel : submodels) {
            submodel.transform.translate(translation);
        }
    }

    public void setPos(Vector3 pos) {
        for (int i = 0; i < submodels.length; i++) {
            submodels[i].transform.setTranslation(pos.add(posOffsets[i]));
        }
    }

    public void scl(float scalar) {
        if (!isOffsetApplied) System.err.println("Warning: ModelGroup " + groupName + " offsets not applied — may be inaccurate!");
        for (ModelInstance submodel : submodels) {
            submodel.transform.scl(scalar);

            Vector3 pos = submodel.transform.getTranslation(new Vector3());
            float deltaSclX = pos.x * scalar - pos.x;
            float deltaSclY = pos.y * scalar - pos.y;
            float deltaSclZ = pos.z * scalar - pos.z;

            submodel.transform.translate(deltaSclX, deltaSclY, deltaSclZ);
        }
    }

    public void func(TriConsumer<ModelInstance, Vector3, Quaternion> func) {
        for (int i = 0; i < submodels.length; i++) {
            func.accept(submodels[i], posOffsets[i], rotOffsets[i]);
        }
    }

    public ModelGroup cpy() {
        ModelGroup copy =  new ModelGroup(submodels, posOffsets, rotOffsets);
        copy.setOffsetApplied(isOffsetApplied);

        return copy;
    }

    public int subCount() {
        return submodels.length;
    }

    public ModelInstance getSubmodel(int index) {
        return submodels[index];
    }

    @Override
    public String toString() {
        return groupName + "( " + Arrays.toString(submodels) + " )";
    }
}
