package pykas0.burn_baby_burn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraftforge.common.Configuration;
import pykas0.burn_baby_burn.common.CommonProxy;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;

@Mod(modid = "burn_baby_burn", name = "Burn Baby Burn", version = "1.0.0")
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
public class BurnBabyBurn {

	@Instance(value = "burn_baby_burn")
	public static BurnBabyBurn instance;

	@SidedProxy(serverSide = "pykaso.burn_baby_burn.common.CommonProxy", clientSide = "pykaso.burn_baby_burn.client.ClientProxy")
	public static CommonProxy proxy;

	private File folder;
	private File config;
	private ArrayList<String> lines;
	private Map<String, BurningProperty> burningProperties;
	private ArrayList<BurningDefinition> burningDefinitions;
	protected int[] newBurnableBlocks;

	@PreInit
	public void preInit(FMLPreInitializationEvent event) {
		this.folder = new File(
				FMLClientHandler.instance().getClient().mcDataDir, "config");
		this.folder.mkdirs();
		this.config = new File(this.folder, "BurnBabyBurn.cfg");
		this.lines = new ArrayList<String>();
		this.burningProperties = new HashMap<String, BurnBabyBurn.BurningProperty>();
		this.burningDefinitions = new ArrayList<BurnBabyBurn.BurningDefinition>();
		boolean exists = false;
		BufferedReader reader = null;
		try {
			exists = this.config.exists();
			if (exists) {
				// if config file exists, read its lines
				reader = new BufferedReader(new FileReader(this.config));
				while (reader.ready()) {
					this.lines.add(reader.readLine());
				}
			} else {
				// if not, create default configs and write them into config
				// file
				this.writeDeafultConfig();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Init
	public void load(FMLInitializationEvent event) {
		proxy.registerRenderers();
	}

	@PostInit
	public void postInit(FMLPostInitializationEvent event) {
		this.loadBurningProperties();
		this.applyBurningProperties();
	}

	private void writeDeafultConfig() {
		this.lines.add("define-plant(60,100)");
		this.lines.add("define-leaves(30,60)");
		this.lines.add("define-wood(5,5)");
		this.lines.add("define-planks(5,20)");
		this.lines.add("define-vine(15,100)");
		this.lines.add("define-cloth(30,60)");
		this.lines.add("plant(6)");
		this.lines.add("plant(37,38)");
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(this.config));
			for (String line : this.lines) {
				writer.write(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	private void loadBurningProperties() {
		for (String line : this.lines) {
			this.handleLine(line);
		}
	}

	private void applyBurningProperties() {
		for (BurningDefinition def : this.burningDefinitions) {
			int id = def.id;
			if (!(Block.blocksList[id] == null)) {
				Block.setBurnProperties(id, def.prop.encouragment,
						def.prop.flammability);
			}
		}
	}

	private void handleLine(String line) {
		if (line.indexOf("-") == -1) {
			// setup of rule
			if (line.indexOf("(") > 0
					&& line.indexOf(")") > (line.length() - 1)) {
				// cut off left side of string
				String[] substrA = line.split(Pattern.quote("("));
				// cut off right side of string
				String substrB = substrA[1].split(Pattern.quote(")"))[0];
				int[] ids;
				String[] strIds = substrB.split(Pattern.quote(","));
				ids = new int[strIds.length];
				for (int i = 0; i < ids.length; i++) {
					try {
						ids[i] = Integer.parseInt(strIds[i]);
						this.burningDefinitions
								.add(new BurningDefinition(ids[0],
										this.burningProperties.get(substrA[0])));
					} catch (NumberFormatException e) {
						System.out.println("burn_baby_burn: '" + strIds[i]
								+ "' is invalid block id");
					}
				}
				/*
				 * if (substrB.indexOf(",") == -1) { ids[0] =
				 * Integer.parseInt(substrB); this.burningDefinitions.add(new
				 * BurningDefinition( ids[0], 0, this.burningProperties
				 * .get(substrA[0]))); } else { String[] res =
				 * substrB.split(Pattern.quote(",")); ids[0] =
				 * Integer.parseInt(res[0]); ids[1] = Integer.parseInt(res[1]);
				 * this.burningDefinitions.add(new BurningDefinition( ids[0],
				 * ids[1], this.burningProperties .get(substrA[0]))); }
				 */

			}
		} else {
			// definition of rule
			if (line.indexOf("define-") == -1)
				return;
			String[] substrA = line.split(Pattern.quote("-"));
			if (substrA.length != 2)
				return;
			if (substrA[1].indexOf("(") == -1 || substrA[1].indexOf(")") == -1
					|| substrA[1].indexOf(",") == -1)
				return;
			String[] substrB = substrA[1].split(Pattern.quote("("));
			if (substrB.length != 2)
				return;
			if (substrB[1].indexOf(")") != (substrB[1].length() - 1))
				return;
			String[] substrC = substrB[1].split(Pattern.quote(")"));
			if (substrC[0].indexOf(",") == -1)
				return;
			String[] substrD = substrC[0].split(Pattern.quote(","));
			if (substrD.length == 2) {
				try {
					int one = Integer.parseInt(substrD[0]);
					int two = Integer.parseInt(substrD[1]);
					this.burningProperties.put(substrB[0], new BurningProperty(
							one, two));
				} catch (NumberFormatException e) {
				}
			}
		}
	}

	private class BurningDefinition {
		int id;
		public BurningProperty prop;

		public BurningDefinition(int id, BurningProperty prop) {
			this.id = id;
			this.prop = prop;
		}
	}

	private class BurningProperty {
		int encouragment;
		int flammability;

		public BurningProperty(int enc, int fla) {
			this.encouragment = enc;
			this.flammability = fla;
		}
	}
}