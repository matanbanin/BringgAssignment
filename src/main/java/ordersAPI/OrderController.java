package ordersAPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The REST server for handling orders
 */

@RestController
public class OrderController {


    private Map orders=new HashMap<String,Order>();
    private HmacUtils hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, "V_-es-3JD82YyiNdzot7");

    /**
     * Creates or updates the customer,
     * Updating Bringg with the new customer data and adding a new order in Bringg
     * @param order The order to be created
     * @return
     * @throws IOException
     */
    @RequestMapping(value="/createOrder",method= RequestMethod.POST)
    public ResponseEntity<Order> createOrder(@RequestBody Order order) throws IOException {
        orders.put(order.getPhone(),order);

        int id = createCustomer(order);
        createOrder(order, id);
        return new ResponseEntity<Order>(order, HttpStatus.OK);
    }

    /**
     * Recreates all the orders from the ast week given a certain phone of a customer
     * @param phone The phone of the customer to recreate it's order
     */
    @RequestMapping(value="/orders",method= RequestMethod.GET)
    public void getOrders(@RequestParam(value="phone") String phone) throws IOException {
        phone='+'+phone;
        getOrdersFromBringg(phone);
    }

    private void getOrdersFromBringg(String phone) throws IOException {
        String url = "https://developer-api.bringg.com/partner_api/tasks/22318742";

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());
        JsonObject jsonToSend = createBaseJson();
        jsonToSend.addProperty("phone",phone);
        System.out.println(phone);
        System.out.println(jsonToSend);
//        JsonArray response = sendJson(con, jsonToSend).getAsJsonArray();
        System.out.println(response);
//        IntStream.range(0,response.size()-1).mapToObj(i->response.get(i).getAsJsonObject()).
//                filter(order->order.get("company_id").);

    }

    /**
     * Create a customer at Bringg
     * @param order
     * @return
     * @throws IOException
     */
    private int createCustomer(Order order) throws IOException {

        HttpURLConnection con = createConnection(new URL("https://developer-api.bringg.com/partner_api/customers/"), RequestMethod.POST);

        JsonObject json = createBaseOrderJson(order);
        json.addProperty("phone",order.getPhone());

        JsonObject responseJson=sendJson(con,json);
        return responseJson.getAsJsonObject("customer").get("id").getAsInt();
    }

    /**
     * Create a Task at Bringg
     * @param order
     * @param id
     * @throws IOException
     */
    private void createOrder(Order order, int id) throws IOException {
        HttpURLConnection con = createConnection(new URL("https://developer-api.bringg.com/partner_api/tasks/"), RequestMethod.POST);

        JsonObject json = createBaseOrderJson(order);
        json.addProperty("customer_id",id);

        sendJson(con, json);
    }

    /**
     * Creates the base JSON of an order or customer to send to Bringg
     * @param order
     * @return
     */
    private JsonObject createBaseOrderJson(Order order) {
        JsonObject json = createBaseJson();
        json.addProperty("name", order.getName());
        json.addProperty("address", order.getAddress());
        return json;
    }

    private JsonObject createBaseJson() {
        JsonObject json = new JsonObject();

        json.addProperty("company_id", 11010);
        json.addProperty("timestamp", Calendar.getInstance().getTimeInMillis() + "");
        json.addProperty("access_token", "ZtWsDxzfTTkGnnsjp8yC");
        return json;
    }

    /**
     * Send a specific Json to a given URL connection, also signs it with hmacHex
     * @param con
     * @param json
     * @return the response JSON
     * @throws IOException
     */
    private JsonObject sendJson(HttpURLConnection con, JsonObject json) throws IOException {
        String signature = hmacUtils.hmacHex(createQueryParams(json));
        json.addProperty("signature",signature);

        try(OutputStream os = con.getOutputStream()) {
            byte[] input = json.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }catch (Throwable e){
        }

        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            return new Gson().fromJson (response.toString(), JsonElement.class).getAsJsonObject();
        }
    }

    /**
     * convert the Json to query params with & in between, mainly for signing
     * @param json
     * @return
     */
    private String createQueryParams(JsonObject json) {
        return json.keySet().stream().
                map(key->key+'='+json.get(key).getAsString()).
                reduce((value1,value2)->value1+'&'+value2).get();
    }

    /**
     * creates an HttpHRLConnection for a given url and request method
     * @param url
     * @param requestMethod
     * @return
     * @throws IOException
     */
    private HttpURLConnection createConnection(URL url, RequestMethod requestMethod) throws IOException {
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod(requestMethod.toString());
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        con.setAllowUserInteraction(true);
        return con;
    }
}


//curl --include --request POST --header "Content-Type: application/json" --data-binary "{ \"company_id\": 11010, \"name\": \"ordername\", \"address\": \"Baraz\", \"phone\": \"+972546543093\", \"timestamp\":\"1556401119123\", \"access_token\":\"ZtWsDxzfTTkGnnsjp8yC\", \"signature\":\"376d1a6427f100e84103835e11ab269bd3adfcd0\"}" https://api.bringg.com/partner_api/customers/