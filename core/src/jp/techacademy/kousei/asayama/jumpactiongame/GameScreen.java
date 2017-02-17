package jp.techacademy.kousei.asayama.jumpactiongame;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class GameScreen extends ScreenAdapter{
    static final float CAMERA_WIDTH = 10;
    static final float CAMERA_HEIGHT = 15;
    static final float WORLD_WIDTH = 10;
    static final float WORLD_HEIGHT = 15* 20;   //20画面分登れば終了
    static final float GUI_WIDTH = 320;
    static final float GUI_HEIGHT = 480;

    static final int GAME_STATE_READY = 0;
    static final int GAME_STATE_PLAYING = 1;
    static final int GAME_STATE_GAMEOVER = 2;

    //重力
    static final float GRAVITY = -12;

    private JumpActionGame mGame;

    Sprite mBg;
    OrthographicCamera mCamera;
    OrthographicCamera mGuiCamera;

    FitViewport mViewPort;
    FitViewport mGuiViewPort;

    Random mRandom;
    List<Step> mSteps;
    List<Star> mStars;
    Ufo mUfo;
    Player mPlayer;

    //Enemy情報追加
    List<Enemy> mEnemies;

    float mHeightSoFar;
    int mGameState;
    Vector3 mTouchPoint;
    BitmapFont mFont;
    int mScore;
    int mHighScore;

    Sound sound ;

    Preferences mPrefs;


    //コンストラクタ
    public GameScreen(JumpActionGame game){
        mGame = game;

        //背景の準備
        Texture bgTexture = new Texture("back.png");
        //TextureRegionで切り出す時の原点は左上
        mBg = new Sprite(new TextureRegion(bgTexture, 0, 0, 540, 810));
        mBg.setSize(CAMERA_WIDTH, CAMERA_HEIGHT);
        mBg.setPosition(0,0);

        //カメラ、ViewPortを生成、設定
        mCamera = new OrthographicCamera();
        mCamera.setToOrtho(false, CAMERA_WIDTH, CAMERA_HEIGHT);
        mViewPort = new FitViewport(CAMERA_WIDTH, CAMERA_HEIGHT, mCamera);

        //GUI用のカメラを設定する
        mGuiCamera = new OrthographicCamera();
        mGuiCamera.setToOrtho(false, GUI_WIDTH, GUI_HEIGHT);
        mGuiViewPort = new FitViewport(GUI_WIDTH, GUI_HEIGHT, mGuiCamera);

        //メンバ変数の初期化
        mRandom = new Random();
        mSteps = new ArrayList<Step>();
        mStars = new ArrayList<Star>();
        mGameState = GAME_STATE_READY;
        mTouchPoint = new Vector3();    //newして生成する

        //Enemyメンバ変数の初期化
        mEnemies = new ArrayList<Enemy>();

        //効果音
        sound = Gdx.audio.newSound(Gdx.files.internal("gameover.mp3"));


        mFont = new BitmapFont(Gdx.files.internal("font.fnt"), Gdx.files.internal("font.png"), false);
        mScore = 0;
        mHighScore = 0;

        //ハイスコアをPreferencesから取得する
        mPrefs = Gdx.app.getPreferences("jp.techacademy.kousei.asayama.jumpactiongame");
        mHighScore = mPrefs.getInteger("HIGHSCORE", 0);

        createStage();
    }

    //描画
    @Override
    public void render (float delta){
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //それぞれの状態をアップデートする
        update(delta);

        //カメラの中心を超えたらカメラを上に移動させる　つまりキャラが画面の上半分には絶対に行かない
        if (mPlayer.getY() > mCamera.position.y){
            mCamera.position.y = mPlayer.getY();
        }

        //カメラの座標をアップデート（計算）し、スプライトの表示に反映させる
        mCamera.update();
        mGame.batch.setProjectionMatrix(mCamera.combined);

        mGame.batch.begin();

        //背景
        //原点は左下
        mBg.setPosition(mCamera.position.x - CAMERA_WIDTH / 2,mCamera.position.y - CAMERA_HEIGHT / 2);
        mBg.draw(mGame.batch);

        //Step
        for (int i = 0; i < mSteps.size(); i++){
            mSteps.get(i).draw(mGame.batch);
        }

        //Enemy
        for (int i = 0 ; i < mEnemies.size(); i++){
            mEnemies.get(i).draw(mGame.batch);
        }

        //Star
        for(int i = 0; i < mStars.size(); i++){
            mStars.get(i).draw(mGame.batch);
        }

        //UFO
        mUfo.draw(mGame.batch);

        //Player
        mPlayer.draw(mGame.batch);

        mGame.batch.end();

        //スコア表示
        mGuiCamera.update();
        mGame.batch.setProjectionMatrix(mGuiCamera.combined);
        mGame.batch.begin();
        mFont.draw(mGame.batch, "HighScore:" + mHighScore, 16, GUI_HEIGHT - 15);
        mFont.draw(mGame.batch, "Score:" + mScore, 16, GUI_HEIGHT - 35);
        mGame.batch.end();
    }

    @Override
    public void resize(int width, int height){
        mViewPort.update(width, height);
        mGuiViewPort.update(width, height);
    }

    //ステージを作成する
    private void createStage(){

        //テクスチャの準備
        Texture stepTexture = new Texture("step.png");
        Texture starTexture = new Texture("star.png");
        Texture playerTexture = new Texture("uma.png");
        Texture ufoTexture = new Texture("ufo.png");

        //Enemyの追加
        Texture enemyTexture = new Texture("enemy.png");

        //StepとStarをゴールの高さまで配置していく
        //Enemyも配置していく
        float y = 0;
        float ey = 0;

        float maxJumpHeight = Player.PLAYER_JUMP_VELOCITY * Player.PLAYER_MOVE_VELOCITY / (2 * -GRAVITY);
        while(y < WORLD_HEIGHT - 5){
            int type = mRandom.nextFloat() > 0.5f ? Step.STEP_TYPE_MOVING : Step.STEP_TYPE_STATIC;
            float x = mRandom.nextFloat() * (WORLD_WIDTH - Step.STEP_WIDTH);

            int etype = mRandom.nextFloat() > 0.4f ? Enemy.ENEMY_TYPE_MOVING : Enemy.ENEMY_TYPE_STATIC;
            float ex = mRandom.nextFloat() / 2 * (WORLD_WIDTH - Enemy.ENEMY_WIDTH);

            Step step = new Step(type, stepTexture, 0, 0, 144, 36);
            step.setPosition(x, y);
            mSteps.add(step);

            Enemy enemy = new Enemy(etype, enemyTexture, 0, 0, 72, 72);
            enemy.setPosition(ex, ey);
            mEnemies.add(enemy);

            if (mRandom.nextFloat() > 0.6f){
                Star star = new Star(starTexture, 0, 0, 72, 72);
                star.setPosition(step.getX() + mRandom.nextFloat(), step.getY() + Star.STAR_HEIGHT + mRandom.nextFloat() * 3);
                mStars.add(star);
            }

            y += (maxJumpHeight - 0.5f);
            y -= mRandom.nextFloat() * (maxJumpHeight / 3);

            ey += (maxJumpHeight - 0.6f);
            ey -= mRandom.nextFloat() * (maxJumpHeight / 4);
        }

        //Playerを配置
        mPlayer = new Player(playerTexture, 0, 0, 72, 72);
        mPlayer.setPosition(WORLD_WIDTH / 2 - mPlayer.getWidth() / 2, Step.STEP_HEIGHT);

        //ゴールのUFOを配置
        mUfo = new Ufo(ufoTexture, 0, 0, 120, 74);
        mUfo.setPosition(WORLD_WIDTH / 2 - Ufo.UFO_WIDTH / 2, y);
    }

    //それぞれのオブジェクトの状態をアップデートする
    private void update(float delta){
        switch (mGameState){
            case GAME_STATE_READY:
                updateReady();
                break;
            case GAME_STATE_PLAYING:
                updatePlaying(delta);
                break;
            case GAME_STATE_GAMEOVER:
                updateGameOver();
                break;
        }
    }

    private void updateReady(){
        if (Gdx.input.justTouched()){
            mGameState = GAME_STATE_PLAYING;
        }
    }

    private void updatePlaying(float delta) {
        float accel = 0;
        if (Gdx.input.isTouched()) {
            mGuiViewPort.unproject(mTouchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0));
            Rectangle left = new Rectangle(0, 0, GUI_WIDTH / 2, GUI_HEIGHT);
            Rectangle right = new Rectangle(GUI_WIDTH / 2, 0, GUI_WIDTH / 2, GUI_HEIGHT);
            if (left.contains(mTouchPoint.x, mTouchPoint.y)) {
                accel = 5.0f;
            }
            if (right.contains(mTouchPoint.x, mTouchPoint.y)) {
                accel = -5.0f;
            }
        }

        // Step
        for (int i = 0; i < mSteps.size(); i++) {
            mSteps.get(i).update(delta);
        }

        //Enemy
        for (int i = 0; i < mEnemies.size(); i++){
            mEnemies.get(i).update(delta);
        }

        // Player
        if (mPlayer.getY() <= 0.5f) {
            mPlayer.hitStep();
        }
        mPlayer.update(delta, accel);
        mHeightSoFar = Math.max(mPlayer.getY(), mHeightSoFar);

        //当たり判定を行う
        checkCollisiton();

        //ゲームオーバーか判断する
        checkGameOver();
    }

    private void checkCollisiton(){
        //UFO（ゴールとの当たり判定
        if (mPlayer.getBoundingRectangle().overlaps(mUfo.getBoundingRectangle())){
            mGameState = GAME_STATE_GAMEOVER;
            return;
        }

        //Starとの当たり判定
        for (int i = 0; i < mStars.size(); i++){
            Star star = mStars.get(i);

            if (star.mState == Star.STAR_NONE){
                continue;
            }

            if (mPlayer.getBoundingRectangle().overlaps(star.getBoundingRectangle())){
                star.get();
                mScore++;
                if (mScore > mHighScore) {
                    mHighScore = mScore;
                    //ハイスコアをPreferenceに保存する
                    mPrefs.putInteger("HIGHSCORE", mHighScore);
                    mPrefs.flush();
                }
                break;
            }
        }

        //Enemyとの当たり判定
        for(int i = 0; i < mEnemies.size(); i++){
            Enemy enemy = mEnemies.get(i);

            if (mPlayer.getBoundingRectangle().overlaps(enemy.getBoundingRectangle())){
                attack();
                mGameState = GAME_STATE_GAMEOVER;
                return;
            }
        }

        //Stepとの当たり判定
        //上昇中はStepとの当たり判定を確認しない
        if (mPlayer.velocity.y > 0){
            return;
        }

        for (int i = 0; i < mSteps.size(); i++){
            Step step = mSteps.get(i);

            if (step.mState == Step.STEP_STATIC_VANISH){
                continue;
            }

            if (mPlayer.getY() > step.getY()){
                if (mPlayer.getBoundingRectangle().overlaps(step.getBoundingRectangle())){
                    mPlayer.hitStep();
                    if (mRandom.nextFloat() > 0.5f){
                        step.vanish();
                    }
                    break;
                }
            }
        }
    }

    private void checkGameOver(){
        if (mHeightSoFar - CAMERA_HEIGHT / 2 > mPlayer.getY()){
            Gdx.app.log("JampActionGame", "GAMEOVER");
            mGameState = GAME_STATE_GAMEOVER;
        }
    }

    private void updateGameOver(){
        if (Gdx.input.justTouched()){
            mGame.setScreen(new ResultScreen(mGame, mScore));
        }
    }

    //敵にぶつかったときの効果音
    private void attack(){
        sound.play(1.0f);
        sound.dispose();
    }
}
