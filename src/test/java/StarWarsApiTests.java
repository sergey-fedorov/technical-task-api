import dtos.Character;
import dtos.Film;
import dtos.Films;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.mapper.ObjectMapperType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.hamcrest.Matchers.is;

public class StarWarsApiTests {

    @BeforeClass
    public void restAssuredConfig(){
        RestAssured.baseURI = "https://swapi.dev/api";
        RestAssured.config().objectMapperConfig(new ObjectMapperConfig(ObjectMapperType.GSON));
    }

    @Test
    public void vaderUseCases_tasks1and2and3(){
        Character darthVader = RestAssured.given()
                .queryParams("search", "Vader")
                .get("/people")
                .then()
                .assertThat().statusCode(200)
                .and()
                .body("results.size()", is(1))
                .body("results[0].name", is("Darth Vader"))
                .extract().jsonPath()
                .getObject("results[0]", Character.class);

        System.out.println("Task #1: Person with the name Vader found");

        List<Film> darthVaderFilms = new ArrayList<>();
        for(String filmUrl : darthVader.getFilms()){
            int filmId = Integer.parseInt(filmUrl.replaceAll("\\D", ""));
            Film film = RestAssured.given()
                    .pathParam("id", filmId)
                    .get("/films/{id}")
                    .then()
                    .assertThat().statusCode(200)
                    .extract().response()
                    .as(Film.class);
            darthVaderFilms.add(film);
        }

        Film filmWithLessPlanets = darthVaderFilms.stream().min(Comparator.comparingInt(f -> f.getPlanets().size())).orElseThrow();
        System.out.println("Task #2: Film that Darth Vader joined and has the less planets is -> " + filmWithLessPlanets.getTitle());

        boolean doesFilmStarshipsContainVaderStarships = filmWithLessPlanets.getStarships().containsAll(darthVader.getStarships());
        System.out.println("Task #3: Is Vader's starship on film -> " + doesFilmStarshipsContainVaderStarships);
    }

    @Test
    public void theOldestCharacterPlayedInAllFilms_task4(){
        // Request #1
        Films films = RestAssured.given()
                .get("/films")
                .then()
                .assertThat().statusCode(200)
                .extract().response()
                .as(Films.class);

        // Get list of characters played in all films
        List<List<String>> characterUrlsListByFilm = films.getResults().stream().map(Film::getCharacters).toList();
        List<String> characterUrlsList = characterUrlsListByFilm.get(0);

        for(List<String> list : characterUrlsListByFilm){
            characterUrlsList.retainAll(list);
        }

        // Get each character's details
        // Request #2, #3, #4
        List<Character> characters = new ArrayList<>();
        for(String url : characterUrlsList){
            int personId = Integer.parseInt(url.replaceAll("\\D", ""));
            Character character = RestAssured.given()
                    .pathParam("id", personId)
                    .get("/people/{id}")
                    .then()
                    .assertThat().statusCode(200)
                    .extract().response()
                    .as(Character.class);
            characters.add(character);
        }

        // Get the oldest character
        Character character = characters.stream()
                .filter(ch -> ch.getBirth_year().contains("BBY") || ch.getBirth_year().contains("ABY"))
                .min(Comparator.comparingDouble(ch -> SWYearToDouble(ch.getBirth_year())))
                .orElseThrow(() -> new NoSuchElementException("All characters have invalid birth_year value or not found"));

        System.out.println("Task #4: The oldest character ever played in all Star Wars films is -> " + character.getName());
    }

    @Test
    public void peopleJsonSchemaValidation_task5(){
        RestAssured.given()
                .get("/people/1")
                .then()
                .assertThat().statusCode(200)
                .and()
                .body(matchesJsonSchema(new File("src/test/resources/schemas/people_schema.json")));

        System.out.println("Task #5: People Json schema validation passed");
    }

    public double SWYearToDouble(String year){
        double yearDouble = Double.parseDouble(year.replaceAll("[^0-9.]", ""));
        return year.contains("BBY")? yearDouble * -1 : yearDouble;
    }
}