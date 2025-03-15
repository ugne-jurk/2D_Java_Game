/**
 * @author Ugnė Jurkšaitytė 5 grupė
 */

import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONObject;
import processing.data.JSONArray;
import java.util.HashSet;
import java.util.Set;

/**
 * 2D  žaidimas su sezoninėmis temomis ir lygių progresija.
 * Ypatybės:
 * - Žaidėjo judėjimas su animacija
 * - Susidūrimų aptikimas
 * - Keli lygiai su besikeičiančiais sezonais
 * - Integruotas lygių redaktorius
 * - JSON pagrindu veikiantis žemėlapių įkėlimas/išsaugojimas
 */
public class MyScratch1 extends PApplet {
    PImage img;  // plytelių rinkinys (vasara.png)
    PImage img1; // žaidėjo paveikslėliai (cats.png)
    PImage img2; // rudens plytelių rinkinys (ruduo.png)
    PImage img3; // žiemos plytelių rinkinys (winter.png)

    PImage[] tileImages; // Masyvas, kuriame saugomos atskiros plytelės iš plytelių rinkinio
    private int finishTileId; // Plytelės ID, žyminti lygio pabaigą

    int cols = 15; // Stulpelių skaičius plytelių rinkinyje
    int rows = 9;  // Eilučių skaičius plytelių rinkinyje
    int tileW;     // Vienos plytelės plotis
    int tileH;     // Vienos plytelės aukštis
    float playerX = 200, playerY = 200; // Žaidėjo pozicija
    float speed = 2.5f;                 // Žaidėjo judėjimo greitis
    float velocityX = 0, velocityY = 0; // Žaidėjo greitis
    PImage[][] playerFrames;            // Žaidėjo animacijos kadrai

    boolean editor = false;        // Ar redaktoriaus režimas aktyvus
    int frameIndex = 0;            // Dabartinis animacijos kadras
    int direction = 0;             // Žaidėjo kryptis (0=žemyn, 1=kairėn, 2=dešinėn, 3=aukštyn)
    int animationSpeed = 6;        // Kontroliuoja animacijos greitį
    int animationCounter = 0;      // Animacijos laiko skaitiklis
    boolean movingUp = false, movingDown = false, movingLeft = false, movingRight = false, isMoving = false;
    float cameraX = 0, cameraY = 0; // Kameros pozicija

    int startX, startY;           // Lygio pradžios pozicija
    int currentLevel = 1;         // Dabartinis lygis (1=vasara, 2=ruduo, 3=žiema)
    int selectedTile = 0;         // Šiuo metu pasirinkta plytelė redaktoriaus režime

    int mapWidth;                 // Žemėlapio plotis plytelėmis
    int mapHeight;                // Žemėlapio aukštis plytelėmis
    final int TILE_SIZE = 32;     // Plytelės dydis pikseliais
    int[][] map;                  // 2D masyvas, saugantis žemėlapio duomenis

    /**
     * Konfigūruoja pradinius lango nustatymus.
     */
    public void settings() {
        size(600, 600);
    }

    /**
     * Inicijuoja žaidimo išteklius ir nustatymus.
     * Įkelia plytelių rinkinių paveikslėlius, išgauna atskiras plyteles ir inicijuoja pirmą lygį.
     */
    public void setup() {
        img = loadImage("summer.png");
        img1 = loadImage("cats.png");
        img2 = loadImage("ruduo.png");
        img3 = loadImage("winter.png");

        tileW = img.width / cols;
        tileH = img.height / rows;

        // Išgauna visas plyteles iš plytelių rinkinio
        tileImages = new PImage[cols * rows];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int index = y * cols + x;
                tileImages[index] = img.get(x * tileW, y * tileH, tileW, tileH);
            }
        }

        // Įkelia pradinius žemėlapio duomenis
        loadMapFromJSON("map1.json");

        // Sukuria žaidėjo animacijos kadrus
        playerFrames = new PImage[4][3];
        int playerTileW = img1.width / 3;
        int playerTileH = img1.height / 4;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                playerFrames[i][j] = img1.get(j * playerTileW, i * playerTileH, playerTileW, playerTileH);
            }
        }
    }

    /** Plytelių ID rinkinys, kurios blokuoja žaidėjo judėjimą */
    Set<Integer> solidTiles = new HashSet<>();

    /**
     * Įkelia žemėlapį iš JSON failo.
     * JSON apima žemėlapio duomenis, nepereinamąsias plyteles, pradžios poziciją ir pabaigos plytelės ID.
     *
     * @param fileName JSON failo, kurį reikia įkelti, pavadinimas
     */
    void loadMapFromJSON(String fileName) {
        JSONObject json = loadJSONObject(fileName);
        JSONArray jsonArray = json.getJSONArray("data");
        JSONArray solidArray = json.getJSONArray("solidTiles");

        solidTiles.clear();
        for (int i = 0; i < solidArray.size(); i++) {
            solidTiles.add(solidArray.getInt(i));
        }

        mapHeight = jsonArray.size();
        mapWidth = jsonArray.getJSONArray(0).size(); // Gauna plotį iš pirmos eilutės

        map = new int[mapHeight][mapWidth];
        for (int i = 0; i < mapHeight; i++) {
            JSONArray row = jsonArray.getJSONArray(i);
            for (int j = 0; j < mapWidth; j++) {
                map[i][j] = row.getInt(j);
            }
        }

        JSONArray startArray = json.getJSONArray("start");
        startX = startArray.getInt(0);
        startY = startArray.getInt(1);

        finishTileId = json.getJSONArray("finish").getInt(0);

        // Nustato žaidėją į naują pradžios poziciją
        playerX = startX * TILE_SIZE;
        playerY = startY * TILE_SIZE;

        // Atstato kameros poziciją
        cameraX = playerX - width / 2;
        cameraY = playerY - height / 2;
    }

    /**
     * Pagrindinis piešimo ciklas, iškviečiamas kiekvieno kadro metu.
     * Tvarko žemėlapį, kai redaktorius aktyvus.
     */
    public void draw() {
        background(0);

        if (editor) {
            drawEditor();
        } else {

            drawMap();
            updateMovement();


            isMoving = (velocityX != 0 || velocityY != 0);
            if (isMoving) {
                animationCounter++;
                if (animationCounter >= animationSpeed) {
                    frameIndex = (frameIndex + 1) % 3;
                    animationCounter = 0;
                }
            } else {
                frameIndex = 0;
            }

            float scaleFactor = 1F;
            image(playerFrames[direction][frameIndex], playerX - cameraX, playerY - cameraY, tileW * scaleFactor, tileH * scaleFactor);
        }
    }

    /**
     * Piešia lygių redaktorių.
     * Rodo žemėlapį su tinklelio perdanga, plytelių paletę ir pasirinktą plytelę.
     */
    private void drawEditor() {
        // Pirmiausia nupiešia žemėlapį
        drawMap();

        // Piešia tinklelio perdangą ant žemėlapio, kad matytų, kur bus dedamos plytelės
        stroke(255, 100);
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                noFill();
                rect(x * TILE_SIZE - cameraX, y * TILE_SIZE - cameraY, TILE_SIZE, TILE_SIZE);
            }
        }

        // Piešia plytelių rinkinį ekrano apačioje
        fill(0, 200);
        rect(0, height - 150, width, 150); // Tamsus fonas plytelių rinkinio sričiai

        // Piešia miniatiūrinę plytelių rinkinio versiją apačioje

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int index = y * cols + x;
                image(tileImages[index], 10 + x * 24, height - 140 + y * 24, 24, 24);
            }
        }

        // Paryškina pasirinktą plytelę
        int selectedX = selectedTile % cols;
        int selectedY = selectedTile / cols;
        stroke(255, 0, 0);
        noFill();
        rect(10 + selectedX * 24, height - 140 + selectedY * 24, 24, 24);

        // Rodo šiuo metu pasirinktą plytelę didesniame vaizde
        fill(0, 150);
        rect(width - 90, 10, 80, 80);
        image(tileImages[selectedTile], width - 80, 20, 60, 60);

        // Prideda keletą instrukcijų
        fill(255);
        textSize(17);
        text("Pasirinkite plytelę iš paletės žemiau. Spauskite E norėdami išeiti. ENTER išsaugoti.", 10, 20);
        text("Pasirinktos plytelės ID: " + selectedTile, width - 180, 100);
    }

    /**
     * Tvarko pelės paspaudimus.
     * Redaktoriaus režime pasirenka plyteles arba deda jas ant žemėlapio.
     */
    @Override
    public void mousePressed() {
        if (editor) {
            // Tikrina, ar spaudžiama plytelių pasirinkimo srityje apačioje
            if (mouseY > height - 150) {
                // Apskaičiuoja, kuri plytelė buvo paspausta apatiniame skydelyje
                int tileX = (int)((mouseX - 10) / 24);
                int tileY = (int)((mouseY - (height - 140)) / 24);

                if (tileX >= 0 && tileX < cols && tileY >= 0 && tileY < rows) {
                    selectTile(tileX, tileY);
                }
            }
            // Kitu atveju dedame plytelę ant žemėlapio
            else {
                int gridX = (int)((mouseX + cameraX) / TILE_SIZE);
                int gridY = (int)((mouseY + cameraY) / TILE_SIZE);

                if (gridX >= 0 && gridY >= 0 && gridX < mapWidth && gridY < mapHeight) {
                    placeTile(gridX, gridY);
                }
            }
        }
    }

    /**
     * Pasirenka plytelę iš paletės redaktoriaus režime.
     *
     * @param x X koordinatė plytelių rinkinio tinklelyje
     * @param y Y koordinatė plytelių rinkinio tinklelyje
     */
    void selectTile(int x, int y) {
        int index = x + y * cols;

        if (index < tileImages.length) {
            selectedTile = index;
            println("Pasirinkta plytelė: " + selectedTile);
        }
    }

    /**
     * Padeda šiuo metu pasirinktą plytelę žemėlapyje nurodytose koordinatėse.
     *
     * @param x X koordinatė žemėlapio tinklelyje
     * @param y Y koordinatė žemėlapio tinklelyje
     */
    void placeTile(int x, int y) {
        map[y][x] = selectedTile + 1; // Išsaugo plytelės indeksą žemėlapio masyve
        println("Padėta plytelė " + selectedTile + " koordinatėse: " + x + ", " + y);
    }

    /**
     * Išsaugo dabartinį žemėlapį į JSON failą.
     * Įtraukia žemėlapio duomenis, kietąsias plyteles, pradžios poziciją ir pabaigos plytelės ID.
     *
     * @param fileName JSON failo, kurį reikia išsaugoti, pavadinimas
     */
    void saveMapToJSON(String fileName) {
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        for (int y = 0; y < map.length; y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < map[0].length; x++) {
                row.append(map[y][x]);
            }
            jsonArray.append(row);
        }

        json.setJSONArray("data", jsonArray);

        // Išsaugo nepereinamų plytelių informaciją
        JSONArray solidArray = new JSONArray();
        for (Integer tileId : solidTiles) {
            solidArray.append(tileId);
        }
        json.setJSONArray("solidTiles", solidArray);

        // Išsaugo pradžios poziciją
        JSONArray startArray = new JSONArray();
        startArray.setInt(0, startX);
        startArray.setInt(1, startY);
        json.setJSONArray("start", startArray);

        // Išsaugo pabaigos plytelės ID
        JSONArray finishArray = new JSONArray();
        finishArray.setInt(0, finishTileId);
        json.setJSONArray("finish", finishArray);

        saveJSONObject(json, fileName);
        println("Žemėlapis išsaugotas į " + fileName);
    }

    /**
     * Piešia dabartinį žemėlapį ekrane.
     * Nustato fono spalvą pagal dabartinio lygio temą.
     */
    private void drawMap() {
        // Nustato fono spalvą pagal lygį
        if (currentLevel == 1) {
            background(194, 152, 109); // Vasaros fonas
        } else if (currentLevel == 2) {
            background(110, 146, 77);  // Rudens fonas
        } else if (currentLevel == 3) {
            background(203, 219, 252); // Žiemos fonas - šviesiai mėlynas/baltas
        }

        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                int tileId = map[y][x];
                if (tileId > 0) {
                    int tileIndex = tileId - 1;
                    if (tileIndex >= 0 && tileIndex < tileImages.length) {
                        image(tileImages[tileIndex], x * TILE_SIZE - cameraX, y * TILE_SIZE - cameraY, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
        mapHeight = map.length;
        mapWidth = map[0].length;
    }

    /**
     * Atnaujina žaidėjo judėjimą ir tvarko susidūrimus.
     * Taip pat kontroliuoja kameros judėjimą ir tikrina laimėjimo sąlygą.
     */
    private void updateMovement() {
        float newX = playerX;
        float newY = playerY;

        // Apskaičiuoja potencialią naują poziciją
        if (movingLeft) newX = playerX - speed;
        else if (movingRight) newX = playerX + speed;

        if (movingUp) newY = playerY - speed;
        else if (movingDown) newY = playerY + speed;

        // "Susidūrimo"  dydis
        float playerWidth = tileW * 0.8f;
        float playerHeight = tileH * 0.8f;

        // Tikrina horizontalaus judėjimo susidūrimą
        boolean canMoveX = true;
        if (movingLeft) {
            // Tikrina kairį kraštą
            if (isColliding(newX, playerY) ||
                    isColliding(newX, playerY + playerHeight/2) ||
                    isColliding(newX, playerY + playerHeight)) {
                canMoveX = false;
            }
        } else if (movingRight) {
            // Tikrina dešinį kraštą
            if (isColliding(newX + playerWidth, playerY) ||
                    isColliding(newX + playerWidth, playerY + playerHeight/2) ||
                    isColliding(newX + playerWidth, playerY + playerHeight)) {
                canMoveX = false;
            }
        }

        // Tikrina vertikalaus judėjimo susidūrimą
        boolean canMoveY = true;
        if (movingUp) {
            // Tikrina viršutinį kraštą
            if (isColliding(playerX, newY) ||
                    isColliding(playerX + playerWidth/2, newY) ||
                    isColliding(playerX + playerWidth, newY)) {
                canMoveY = false;
            }
        } else if (movingDown) {
            // Tikrina apatinį kraštą
            if (isColliding(playerX, newY + playerHeight) ||
                    isColliding(playerX + playerWidth/2, newY + playerHeight) ||
                    isColliding(playerX + playerWidth, newY + playerHeight)) {
                canMoveY = false;
            }
        }

        // Pritaiko judėjimą tik jei nėra susidūrimo
        if (canMoveX) {
            playerX = newX;
            velocityX = movingLeft ? -speed : (movingRight ? speed : 0);
        } else {
            velocityX = 0;
        }

        if (canMoveY) {
            playerY = newY;
            velocityY = movingUp ? -speed : (movingDown ? speed : 0);
        } else {
            velocityY = 0;
        }

        // Atnaujina animacijos būseną
        isMoving = (velocityX != 0 || velocityY != 0);

        // Užtikrina, kad žaidėjas liktų žemėlapio ribose
        playerX = constrain(playerX, 0, mapWidth * TILE_SIZE - playerWidth);
        playerY = constrain(playerY, 0, mapHeight * TILE_SIZE - playerHeight);

        // Kamera seka žaidėją
        cameraX = playerX - width / 2;
        cameraY = playerY - height / 2;

        // Išlaiko kamerą ribose
        cameraX = constrain(cameraX, 0, mapWidth * TILE_SIZE - width);
        cameraY = constrain(cameraY, 0, mapHeight * TILE_SIZE - height);

        checkWinCondition();
    }

    /**
     * Tikrina, ar žaidėjas pasiekė pabaigos plytelę.
     * Jei taip, pereina į kitą lygį arba iš naujo pradeda žaidimą.
     */
    private void checkWinCondition() {
        // Gauna žaidėjo centro tašką tikslesniam pozicijos tikrinimui
        int playerTileX = (int) ((playerX + (tileW * 0.75f) / 2) / TILE_SIZE);
        int playerTileY = (int) ((playerY + (tileH * 0.75f) / 2) / TILE_SIZE);

        // Tikrina, ar žaidėjas yra ant pabaigos plytelės
        if (playerTileX >= 0 && playerTileX < mapWidth &&
                playerTileY >= 0 && playerTileY < mapHeight &&
                map[playerTileY][playerTileX] == finishTileId) {

            println("🎉 LAIMĖJOTE! 🎉");

            // Tikrina, ar yra kitas lygis
            if (currentLevel == 1) {
                println("Įkeliamas 2 lygis...");
                nextLevel(2); // Įkelia 2 lygį
            } else if (currentLevel == 2) {
                println("Įkeliamas paskutinis lygis...");
                nextLevel(3); // Įkelia paskutinį lygį
            } else {
                println("Žaidimas baigtas! Pradedama iš naujo...");
                resetGame(); // Iš naujo pradeda žaidimą, jei visi lygiai baigti
            }
        }
    }

    /**
     * Pereina į kitą lygį.
     * Įkelia naujus žemėlapio duomenis ir pakeičia plytelių rinkinį, atitinkantį sezoną.
     *
     * @param level Lygio numeris, kurį reikia įkelti (1=vasara, 2=ruduo, 3=žiema)
     */
    private void nextLevel(int level) {
        currentLevel = level;  // Atnaujina lygio numerį

        if (level == 2) {
            loadMapFromJSON("map2.json");  // Įkelia 2 lygio žemėlapį

            // Persijungia į ruduo.png plytelių rinkinį
            img = img2;

            // Iš naujo išgauna visas plyteles iš naujo plytelių rinkinio
            tileW = img.width / cols;
            tileH = img.height / rows;
        } else if (level == 3) {
            loadMapFromJSON("map3.json");  // Įkelia 3 lygio žemėlapį

            // Persijungia į winter.png plytelių rinkinį
            img = img3;

            // Iš naujo išgauna visas plyteles iš naujo plytelių rinkinio
            tileW = img.width / cols;
            tileH = img.height / rows;
        }

        // Atnaujina plyteles naujam plytelių rinkiniui
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int index = y * cols + x;
                tileImages[index] = img.get(x * tileW, y * tileH, tileW, tileH);
            }
        }
    }

    /**
     * Iš naujo pradeda žaidimą nuo pirmo lygio.
     * Įkelia pradinį žemėlapį ir vasaros plytelių rinkinį.
     */
    private void resetGame() {
        currentLevel = 1;  // Grįžta į 1 lygį
        loadMapFromJSON("map1.json");  // Įkelia pirmą žemėlapį

        // Grįžta į vasaros plytelių rinkinį
        img = loadImage("summer.png");

        // Iš naujo išgauna visas plyteles iš originalaus plytelių rinkinio
        tileW = img.width / cols;
        tileH = img.height / rows;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int index = y * cols + x;
                tileImages[index] = img.get(x * tileW, y * tileH, tileW, tileH);
            }
        }

        playerX = startX * TILE_SIZE;
        playerY = startY * TILE_SIZE;
        cameraX = playerX - width / 2;
        cameraY = playerY - height / 2;
    }

    /**
     * Tikrina, ar taškas susiduria su nepereinama plytele.
     *
     * @param x X koordinatė, kurią reikia patikrinti
     * @param y Y koordinatė, kurią reikia patikrinti
     * @return true, jei taškas susiduria su kietąja plytele, false priešingu atveju
     */
    private boolean isColliding(float x, float y) {
        int tileX = (int) (x / TILE_SIZE);
        int tileY = (int) (y / TILE_SIZE);

        // Neleidžia išeiti už ribų
        if (tileX < 0 || tileY < 0 || tileX >= mapWidth || tileY >= mapHeight) {
            return true;
        }

        int tileId = map[tileY][tileX];

        // Tikrina, ar tai nepereinama plytelė
        return solidTiles.contains(tileId);
    }

    /**
     * Tvarko klavišų paspaudimo įvykius.
     * Kontroliuoja žaidėjo judėjimą, redaktoriaus režimą ir žemėlapių išsaugojimą.
     */
    public void keyPressed() {
        if (key == 'w') {
            movingUp = true;
            direction = 3;
        } else if (key == 's') {
            movingDown = true;
            direction = 0;
        } else if (key == 'a') {
            movingLeft = true;
            direction = 1;
        } else if (key == 'd') {
            movingRight = true;
            direction = 2;
        } else if (key == 'e') {
            editor = !editor;
        }
        else if (key == ENTER) {
            // Išsaugo į teisingą žemėlapio failą pagal dabartinį lygį
            String mapFile = "map" + currentLevel + ".json";
            saveMapToJSON(mapFile);
        }

        if (editor) {
            if (keyCode == LEFT) {
                selectedTile = max(0, selectedTile - 1); // Juda kairėn plytelių pasirinkime
            } else if (keyCode == RIGHT) {
                selectedTile = min(tileImages.length - 1, selectedTile + 1); // Juda dešinėn
            }
        }
    }

    /**
     * Tvarko klavišų atleidimo įvykius.
     * Sustabdo žaidėjo judėjimą, kai judėjimo klavišai atleidžiami.
     */
    public void keyReleased() {
        if (key == 'w') movingUp = false;
        if (key == 's') movingDown = false;
        if (key == 'a') movingLeft = false;
        if (key == 'd') movingRight = false;
    }

    


    /**
     * Pagrindinis metodas, pradedantis programą.
     *
     * @param args Komandinės eilutės argumentai
     */
    public static void main(String[] args) {
        PApplet.main("MyScratch1");
    }
}