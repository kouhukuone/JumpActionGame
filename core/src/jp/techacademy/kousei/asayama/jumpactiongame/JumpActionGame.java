package jp.techacademy.kousei.asayama.jumpactiongame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class JumpActionGame extends Game {
	//public にして外からアクセスできるようにする
	public SpriteBatch batch;
	public ActivityRequestHandler mRequestHandler;

	public JumpActionGame(ActivityRequestHandler requestHandler){
		super();
		mRequestHandler = requestHandler;
	}

	@Override
	public void create() {
		batch = new SpriteBatch();

		//GameScreenを表示
		setScreen(new GameScreen(this));
	}
}

