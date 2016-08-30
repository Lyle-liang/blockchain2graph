package com.oakinvest.b2g.service;

import com.oakinvest.b2g.dto.external.bitcoind.BlockCountResponse;
import com.oakinvest.b2g.dto.external.bitcoind.BlockHashResponse;
import org.apache.commons.codec.binary.Base64;
import org.neo4j.ogm.json.JSONException;
import org.neo4j.ogm.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of bitcoind call.
 * Created by straumat on 26/08/16.
 */
@Service
public class BitcoindServiceImplementation implements BitcoindService {

	/**
	 * Command to get blockcount.
	 */
	private static final String COMMAND_GETBLOCKCOUNT = "getblockcount";

	/**
	 * Comment to get getblockhash.
	 */
	private static final String COMMAND_GETBLOCKHASH = "getblockhash";

	/**
	 * Method parameter.
	 */
	private static final String PARAMETER_METHOD = "method";

	/**
	 * Params parameter.
	 */
	private static final String PARAMETER_PARAMS = "params";

	/**
	 * Logger.
	 */
	private final Logger log = LoggerFactory.getLogger(BitcoindService.class);

	/**
	 * Bitcoind hostname.
	 */
	@Value("${bitcoind.hostname}")
	private String hostname;

	/**
	 * Bitcoind port.
	 */
	@Value("${bitcoind.port}")
	private String port;

	/**
	 * Bitcoind username.
	 */
	@Value("${bitcoind.username}")
	private String username;

	/**
	 * Bitcoind password.
	 */
	@Value("${bitcoind.password}")
	private String password;

	/**
	 * Calling getblockcount on bitcoind server.
	 * curl --user bitcoinrpc:JRkDy3tgCYdmCEqY1VdfdfhTswiRva --data-binary '{"jsonrpc":"1.0","method":"getblockcount","params":[]}' -H 'content-type:text/plain;' -X POST http://5.196.65.205:8332
	 *
	 * @return blockcount.
	 */
	@Override
	public final BlockCountResponse getBlockCount() {
		// FIXME Deal with errors like {"result":null,"error":{"code":-28,"message":"Loading block index..."},"id":null}
		// Configuring the request.
		JSONObject request = new JSONObject();
		try {
			request.put(PARAMETER_METHOD, COMMAND_GETBLOCKCOUNT);
		} catch (JSONException e) {
			log.error("Error while building the request " + e);
			e.printStackTrace();
		}

		// Making the call.
		RestTemplate restTemplate = new RestTemplate();
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), getHeaders());
		log.info("Calling getBlockCount with " + request);
		return restTemplate.postForObject(getURL(), entity, BlockCountResponse.class);
	}


	/**
	 * Calling getblockhash on bitcoind server.
	 * curl --user bitcoinrpc:JRkDy3tgCYdmCEqY1VdfdfhTswiRva --data-binary '{"method": "getblockhash", "params": [1] }' -H 'content-type: text/plain;' -X POST http://5.196.65.205:8332
	 *
	 * @param blockNumber block number.
	 * @return blockhash.
	 */
	@Override
	public BlockHashResponse getBlockHash(final int blockNumber) {
		JSONObject request = new JSONObject();
		try {
			request.put(PARAMETER_METHOD, COMMAND_GETBLOCKHASH);
			List<Integer> params = new ArrayList<>();
			params.add(blockNumber);
			request.put(PARAMETER_PARAMS, params);

		} catch (JSONException e) {
			log.error("Error while building the request " + e);
			e.printStackTrace();
		}

		// Making the call.
		RestTemplate restTemplate = new RestTemplate();
		HttpEntity<String> entity = new HttpEntity<>(request.toString(), getHeaders());
		log.info("Calling getBlockHash on block " + blockNumber);
		//System.out.println(restTemplate.exchange(getURL(), HttpMethod.POST, entity, String.class));
		return restTemplate.postForObject(getURL(), entity, BlockHashResponse.class);

	}

	/**
	 * Getting the URL to call.
	 *
	 * @return bitcoind serveur url
	 */
	private String getURL() {
		return "http://" + hostname + ":" + port;
	}

	/**
	 * Manage authentication.
	 *
	 * @return requireed headers
	 */
	private HttpHeaders getHeaders() {
		return new HttpHeaders() {
			{
				String auth = username + ":" + password;
				byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
				String authHeader = "Basic " + new String(encodedAuth);
				set("Authorization", authHeader);
			}
		};
	}

}