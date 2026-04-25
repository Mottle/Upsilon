package com.github.wintersteve25.tau.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3f;

/**
 * 2D/3D transform wrapper used by {@code Transform} components.
 * <p>
 * Stores both forward and inverse matrices for rendering and input-space mapping.
 */
public class Transformation {

    protected final Matrix4f transform;
    protected final Matrix4f inverted;
    private final Vector3f translation;
    private final boolean translationOnly;

    /**
     * Creates a transformation from forward/inverse matrices.
     */
    protected Transformation(Matrix4f transform, Matrix4f inverted, Vector3f translation, boolean translationOnly) {
        this.transform = transform;
        this.inverted = inverted;
        this.translation = translation;
        this.translationOnly = translationOnly;
    }

    /**
     * Applies this transform to the active pose stack.
     */
    public void transform(PoseStack poseStack) {
        poseStack.mulPose(transform);
    }

    /**
     * Applies inverse transformation to a mutable integer point.
     */
    public void transformPoint(SimpleVec2i pos) {
        // Apply the inverted translation to the mouse position
        Vector3f transformedPos = new Vector3f(pos.x, pos.y, 0.0f); // Convert to 3D for matrix multiplication
        transformedPos.mulPosition(inverted);

        // Update the original position with the transformed coordinates (truncate z for 2D)
        pos.x = (int) Math.floor(transformedPos.x);
        pos.y = (int) Math.floor(transformedPos.y);
    }

    /**
     * Applies inverse transformation to a mutable double point.
     */
    public void transformPoint(Vector2d pos) {
        Vector3f transformedPos = new Vector3f((float) pos.x, (float) pos.y, 0.0f);
        transformedPos.mulPosition(inverted);
        pos.x = transformedPos.x;
        pos.y = transformedPos.y;
    }

    /**
     * Returns whether this transform contains only translation.
     */
    public boolean isTranslationOnly() {
        return translationOnly;
    }

    /**
     * Returns translation vector when available, otherwise zero vector.
     */
    public Vector3f getTranslation() {
        return translation == null ? new Vector3f() : new Vector3f(translation);
    }

    /**
     * Creates a scale transform.
     */
    public static Transformation scale(Vector3f scale) {
        Matrix4f matrix4f = new Matrix4f().identity().scale(scale);
        return new Transformation(matrix4f, new Matrix4f(matrix4f).invert(), null, false);
    }

    /**
     * Creates a translation transform.
     */
    public static Transformation translate(Vector3f translation) {
        Matrix4f matrix4f = new Matrix4f().identity().translation(translation);
        return new Transformation(matrix4f, new Matrix4f(matrix4f).invert(), new Vector3f(translation), true);
    }
}
