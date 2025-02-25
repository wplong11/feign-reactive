/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign.resttemplate.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.ReactiveOptions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;

import static java.util.Optional.ofNullable;

/**
 * {@link RestTemplate} based implementation
 *
 * @author Sergii Karpenko
 */
public class RestTemplateFakeReactiveFeign {

  public static <T> ReactiveFeign.Builder<T> builder() {

    return new ReactiveFeign.Builder<T>(){
      private RestTemplate restTemplate = new RestTemplate();
      private boolean acceptGzip = false;

      {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
        restTemplate.getMessageConverters().add(0, converter);
        updateClientFactory();
      }

      private void updateClientFactory(){
        clientFactory(methodMetadata -> new RestTemplateFakeReactiveHttpClient(
                methodMetadata, restTemplate, acceptGzip));
      }

      @Override
      public ReactiveFeignBuilder<T> objectMapper(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        restTemplate.getMessageConverters().set(0, converter);
        updateClientFactory();
        return this;
      }

      @Override
      public ReactiveFeign.Builder<T> options(ReactiveOptions options) {

        if(options.isFollowRedirects() != null || options.getProxySettings() != null){
          SimpleClientHttpRequestFactory requestFactory;
          if(options.isFollowRedirects() != null){
            requestFactory = new SimpleClientHttpRequestFactory(){
              @Override
              protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(options.isFollowRedirects());
              }
            };
          } else {
            requestFactory = new SimpleClientHttpRequestFactory();
          }

          ReactiveOptions.ProxySettings proxySettings = options.getProxySettings();
          if(proxySettings != null){
            requestFactory.setProxy(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxySettings.getHost(), proxySettings.getPort())));
          }

          if (options.getConnectTimeoutMillis() != null) {
            requestFactory.setConnectTimeout(options.getConnectTimeoutMillis().intValue());
          }

          RestTemplateReactiveOptions restTemplateOptions = (RestTemplateReactiveOptions)options;
          if (restTemplateOptions.getReadTimeoutMillis() != null) {
            requestFactory.setReadTimeout(restTemplateOptions.getReadTimeoutMillis().intValue());
          }

          this.restTemplate = new RestTemplate(requestFactory);
          this.acceptGzip = ofNullable(options.isTryUseCompression()).orElse(false);
          updateClientFactory();
          return this;

        }

        else {

          HttpComponentsClientHttpRequestFactory requestFactory =
                  new HttpComponentsClientHttpRequestFactory();
          if (options.getConnectTimeoutMillis() != null) {
            requestFactory.setConnectTimeout(options.getConnectTimeoutMillis().intValue());
          }

          RestTemplateReactiveOptions restTemplateOptions = (RestTemplateReactiveOptions)options;
          if (restTemplateOptions.getReadTimeoutMillis() != null) {
            requestFactory.setReadTimeout(restTemplateOptions.getReadTimeoutMillis().intValue());
          }

          this.restTemplate = new RestTemplate(requestFactory);
          this.acceptGzip = ofNullable(options.isTryUseCompression()).orElse(false);
          updateClientFactory();
          return this;

        }
      }
    };
  }
}


