package io.github.pocketrice.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import io.github.pocketrice.client.SchuAssetManager;
import io.github.pocketrice.client.SchuGame;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.NotImplementedException;

/*
 * @author ronrihoo
 */
public class BackgroundColor implements Drawable {
	@Getter @Setter
	private float x, y, width, height;
	@Getter @Setter
	private boolean isFillParent;

	@Getter @Setter
	private Texture texture;
	private TextureRegion textureRegion;
	private Sprite sprite;
	private Color color;
	
	public BackgroundColor(Texture t) {
		this(t, 0.0f, 0.0f, 0.0f, 0.0f);
	}
	
	public BackgroundColor(Texture t, float x, float y) {
		this(t, x, y, 0.0f, 0.0f);
	}
	
	public BackgroundColor(Texture t, float x, float y, float width, float height) {
		setTexture(t);
		setPosition(x,y);
		if (width < 0.0f || height < 0.0f)
			setSize();	// width = 0.0f; height = 0.0f;
		if (color == null)
			setColor(255, 255, 255, 255);
		if (sprite == null) {
			try {
				setSprite();
			} catch (Exception e) {
				System.err.println(e);
			}
		}
		isFillParent = true;
	}
	
	private void setTextureRegion() {
		textureRegion = new TextureRegion(texture, (int) getWidth(), (int) getHeight());
	}
	
	private void setSprite() {
		setTextureRegion();
		sprite = new Sprite(textureRegion);
		setSpriteColor();
	}
	
	private void setSpriteColor() {
		sprite.setColor(color.r, color.g, color.b, color.a);
	}

	private void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	private void setSize() {
		this.width = sprite.getWidth();
		this.height = sprite.getHeight();
	}
	
	private void setSize(float width, float height) {
		this.width = width;
		this.height = height;
	}

	public void setColor(float r, float g, float b, float a) {
		color = new Color(r/255f, g/255f, b/255f, a/255f);
		if (sprite != null) {
			setSpriteColor();
		}
	}

	public void setColor(Color col) {
		setColor(col.r, col.g, col.b, col.a);
	}
	
	private void setSpritePosition(float x, float y) {
		sprite.setX(x);
		sprite.setY(y);
	}
	
	private void updateSprite(float x, float y, float width, float height) {
		if (isFillParent) {
			setSpritePosition(x, y);
			if (width != textureRegion.getRegionWidth() || 
					height != textureRegion.getRegionHeight()) {
				setSize(width, height);
				setSprite();
			}
		}
	}
	
	@Override
	public void draw(Batch batch, float x, float y, float width, float height) {
		updateSprite(x, y, width, height);
		sprite.draw(batch);
	}

	@Override
	public float getLeftWidth() {
		throw new NotImplementedException();
	}

	@Override
	public void setLeftWidth(float leftWidth) {
		throw new NotImplementedException();
	}

	@Override
	public float getRightWidth() {
		throw new NotImplementedException();
	}

	@Override
	public void setRightWidth(float rightWidth) {
		throw new NotImplementedException();
	}

	@Override
	public float getTopHeight() {
		throw new NotImplementedException();
	}

	@Override
	public void setTopHeight(float topHeight) {
		throw new NotImplementedException();
	}

	@Override
	public float getBottomHeight() {
		throw new NotImplementedException();
	}

	@Override
	public void setBottomHeight(float bottomHeight) {
		throw new NotImplementedException();
	}

	@Override
	public float getMinWidth() {
		throw new NotImplementedException();
	}

	@Override
	public void setMinWidth(float minWidth) {
		throw new NotImplementedException();
	}

	@Override
	public float getMinHeight() {
		throw new NotImplementedException();
	}

	@Override
	public void setMinHeight(float minHeight) {
		throw new NotImplementedException();
	}

	public static BackgroundColor generateSolidBg(Color col) {
		SchuAssetManager amgr = SchuGame.getGlobalAmgr();
		if (!amgr.aliasedContains("main.atlas")) System.err.println("Error: Solid background default texture not in project!");
		BackgroundColor bgc = new BackgroundColor(amgr.aliasedGet("main.atlas", TextureAtlas.class).findRegion("1px").getTexture());
		bgc.setColor(col);
		return bgc;
	}
}
