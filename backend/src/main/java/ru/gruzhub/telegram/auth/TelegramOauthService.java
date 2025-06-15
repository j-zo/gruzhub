package ru.gruzhub.telegram.auth;

import jakarta.ws.rs.ForbiddenException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.TreeSet;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gruzhub.telegram.auth.dto.TelegramOauthDataDto;
import ru.gruzhub.tools.env.EnvVariables;

@Service
@RequiredArgsConstructor
public class TelegramOauthService {
    private final EnvVariables envVariables;

    public TelegramOauthDataDto getAuthData(String queryParams) {
        try {
            TelegramOauthDataDto authData = new TelegramOauthDataDto();
            String hash = null;

            String[] params = queryParams.split("&");
            Set<String> set = new TreeSet<>();

            for (String p : params) {

                if (p.startsWith("hash=")) {
                    hash = p.substring(5);
                } else {
                    set.add(p);
                }

                if (p.startsWith("id")) {
                    authData.setId(Long.parseLong(p.split("=")[1]));
                }
            }

            String dataCheck = String.join("\n", set);

            byte[] secret = this.sha256(this.envVariables.TELEGRAM_BOT_TOKEN.getBytes());
            String result = this.hmacSha256(secret, dataCheck);

            boolean isValidAuth = result.equals(hash);

            if (!isValidAuth) {
                throw new ForbiddenException("Auth data is not valid");
            }

            return authData;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] sha256(byte[] string) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(string);
    }

    private String hmacSha256(byte[] key, String data)
        throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
        hmacSha256.init(secretKey);
        byte[] result = hmacSha256.doFinal(data.getBytes());
        return this.hex(result);

    }

    private String hex(byte[] str) {
        return String.format("%040x", new BigInteger(1, str));
    }

}
