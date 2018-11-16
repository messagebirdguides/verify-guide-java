import java.util.HashMap;
import java.util.Map;

import com.messagebird.MessageBirdClient;
import com.messagebird.MessageBirdService;
import com.messagebird.MessageBirdServiceImpl;

import com.messagebird.objects.Verify;
import com.messagebird.objects.VerifyRequest;
import com.messagebird.objects.VerifyType;
import com.messagebird.exceptions.GeneralException;
import com.messagebird.exceptions.UnauthorizedException;

import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import static spark.Spark.get;
import static spark.Spark.post;

import io.github.cdimascio.dotenv.Dotenv;


public class TwoFA {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        // Create a MessageBirdService
        final MessageBirdService messageBirdService = new MessageBirdServiceImpl(dotenv.get("MESSAGEBIRD_API_KEY"));
        // Add the service to the client
        final MessageBirdClient messageBirdClient = new MessageBirdClient(messageBirdService);

        get("/",
                (req, res) ->
                {
                    return new ModelAndView(null, "step1.mustache");
                },
                new MustacheTemplateEngine()
        );

        post("/step2",
                (req, res) ->
                {
                    String number = req.queryParams("number");
                    Map<String, Object> model = new HashMap<>();

                    try {
                        VerifyRequest verifyRequest = new VerifyRequest(number);
                        verifyRequest.setTimeout(120);

                        final Verify verify = messageBirdClient.sendVerifyToken(verifyRequest);

                        model.put("otpId", verify.getId());

                        return new ModelAndView(model, "step2.mustache");
                    } catch (UnauthorizedException | GeneralException ex) {
                        model.put("errors", ex.toString());
                        return new ModelAndView(model, "step2.mustache");
                    }
                },
                new MustacheTemplateEngine()
        );


        post("/step3",
                (req, res) ->
                {
                    String id = req.queryParams("id");
                    String token = req.queryParams("token");

                    Map<String, Object> model = new HashMap<>();

                    try {
                        final Verify verify = messageBirdClient.verifyToken(id, token);

                        return new ModelAndView(model, "step3.mustache");
                    } catch (UnauthorizedException | GeneralException ex) {
                        model.put("errors", ex.toString());
                        return new ModelAndView(model, "step2.mustache");
                    }
                },
                new MustacheTemplateEngine()
        );
    }
}