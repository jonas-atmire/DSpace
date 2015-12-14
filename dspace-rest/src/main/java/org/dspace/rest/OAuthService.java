package org.dspace.rest;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import sun.security.krb5.internal.crypto.Des;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jonas - jonas@atmire.com on 10/12/15.
 */
public class OAuthService {


    private String sparklrPhotoListURL ="@/photos/1";
    private String sparklrTrustedMessageURL;
    private String sparklrPhotoURLPattern="http://localhost:8080/dspace-oauth/photos/1";
    private RestOperations sparklrRestTemplate ;
    private RestOperations trustedClientRestTemplate;

    public String getAccessToken()  {


        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();


        resource.setAccessTokenUri("http://localhost:8080/dspace-oauth/oauth/token");

        OAuth2RestTemplate template = new OAuth2RestTemplate(resource);

       OAuth2AccessToken token = template.getAccessToken();


        sparklrRestTemplate = createRestTemplate();
        sparklrRestTemplate = template;

        InputStream photosXML = new ByteArrayInputStream(sparklrRestTemplate.getForObject(
                URI.create("http://localhost:8080/dspace-oauth/photos/1"), byte[].class));

        File targetFile = new File("src/main/resources/targetFile.png");

        try {
            FileUtils.copyInputStreamToFile(photosXML, targetFile);
            Desktop.getDesktop().open(targetFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final List<String> photoIds = new ArrayList<String>();
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setValidating(false);
            parserFactory.setXIncludeAware(false);
            parserFactory.setNamespaceAware(false);
        try {
            SAXParser parser = parserFactory.newSAXParser();
            parser.parse(photosXML, new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    if ("photo".equals(qName)) {
                        photoIds.add(attributes.getValue("id"));
                    }
                }
            });
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return photoIds.toString();

    }

    public InputStream loadSparklrPhoto(String id) {
        return new ByteArrayInputStream(sparklrRestTemplate.getForObject(
                URI.create(String.format(sparklrPhotoURLPattern, id)), byte[].class));
    }

    public String getTrustedMessage() {
        return this.trustedClientRestTemplate.getForObject(URI.create(sparklrTrustedMessageURL), String.class);
    }

    public void setSparklrPhotoURLPattern(String sparklrPhotoURLPattern) {
        this.sparklrPhotoURLPattern = sparklrPhotoURLPattern;
    }

    public void setSparklrPhotoListURL(String sparklrPhotoListURL) {
        this.sparklrPhotoListURL = sparklrPhotoListURL;
    }

    public void setSparklrTrustedMessageURL(String sparklrTrustedMessageURL) {
        this.sparklrTrustedMessageURL = sparklrTrustedMessageURL;
    }

    public void setSparklrRestTemplate(RestOperations sparklrRestTemplate) {
        this.sparklrRestTemplate = sparklrRestTemplate;
    }

    public void setTrustedClientRestTemplate(RestOperations trustedClientRestTemplate) {
        this.trustedClientRestTemplate = trustedClientRestTemplate;
    }
    public RestOperations createRestTemplate() {
        RestTemplate client = new RestTemplate();
        client.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
            @Override
            protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
                HttpClientContext context = HttpClientContext.create();
                RequestConfig.Builder builder = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                        .setAuthenticationEnabled(false).setRedirectsEnabled(false);
                context.setRequestConfig(builder.build());
                return context;
            }
        });
        client.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        return client;
    }
}
