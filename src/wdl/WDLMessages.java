package wdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

import wdl.api.IWDLMessageType;
import wdl.api.IWDLMod;

/**
 * Handles enabling and disabling of all of the messages.
 */
public class WDLMessages {
	/**
	 * Information about an individual message type.
	 */
	private static class MessageRegistration {
		public final String name;
		public final IWDLMessageType type;
		public final String owner;
		
		/**
		 * Creates a MessageRegistration.
		 * 
		 * @param name The name to use.
		 * @param type The type bound to this registration.
		 * @param owner The name of the mod adding it.  ("Core" for base WDL)
		 */
		public MessageRegistration(String name, IWDLMessageType type,
				String owner) {
			this.name = name;
			this.type = type;
			this.owner = owner;
		}
		
		@Override
		public String toString() {
			return "MessageRegistration [name=" + name + ", type=" + type
					+ ", owner=" + owner + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((owner == null) ? 0 : owner.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof MessageRegistration)) {
				return false;
			}
			MessageRegistration other = (MessageRegistration) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (owner == null) {
				if (other.owner != null) {
					return false;
				}
			} else if (!owner.equals(other.owner)) {
				return false;
			}
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (!type.equals(other.type)) {
				return false;
			}
			return true;
		}
	}
	
	/**
	 * If <code>false</code>, all messages are disabled.  Otherwise, per-
	 * message settings are used.
	 */
	public static boolean enableAllMessages = true;
	
	/**
	 * List of all registrations.
	 */
	private static List<MessageRegistration> registrations =
			new ArrayList<MessageRegistration>();
	
	/**
	 * Gets the {@link MessageRegistration} for the given name.
	 * @param name
	 * @return The registration or null if none is found.
	 */
	private static MessageRegistration getRegistration(String name) {
		for (MessageRegistration r : registrations) {
			if (r.name.equals(name)) {
				return r;
			}
		}
		return null;
	}
	
	/**
	 * Gets the {@link MessageRegistration} for the given {@link IWDLMessageType}.
	 * @param name
	 * @return The registration or null if none is found.
	 */
	private static MessageRegistration getRegistration(IWDLMessageType type) {
		for (MessageRegistration r : registrations) {
			if (r.type.equals(type)) {
				return r;
			}
		}
		return null;
	}
	
	/**
	 * Adds registration for another type of message.
	 * 
	 * @param name The programmatic name.
	 * @param type The type.
	 * @param owner The owning mod (used as a category).
	 */
	public static void registerMessage(String name, IWDLMessageType type,
			String owner) {
		registrations.add(new MessageRegistration(name, type, owner));
	}
	
	/**
	 * Is the specified type enabled?
	 */
	public static boolean isEnabled(IWDLMessageType type) {
		if (type == null) {
			return false;
		}
		if (!enableAllMessages) {
			return false;
		}
		MessageRegistration r = getRegistration(type);
		if (r == null) {
			return false;
		}
		
		if (!WDL.baseProps.containsKey("Messages." + r.name)) {
			if (WDL.baseProps.containsKey("Debug." + r.name)) {
				//Updating from older version
				WDL.baseProps.put("Messages." + r.name,
						WDL.baseProps.remove("Debug." + r.name));
			} else {
				WDL.baseProps.setProperty("Messages." + r.name,
						Boolean.toString(r.type.isEnabledByDefault()));
			}
		}
		return WDL.baseProps.getProperty("Messages." + r.name).equals("true");
	}
	
	/**
	 * Toggles whether the given type is enabled.
	 * @param type
	 */
	public static void toggleEnabled(IWDLMessageType type) {
		MessageRegistration r = getRegistration(type);
		
		if (r != null) {
			WDL.baseProps.setProperty("Messages." + r.name,
					Boolean.toString(!isEnabled(type)));
		}
	}
	
	/**
	 * Gets all of the MessageTypes 
	 * @return All the types, ordered by the creating mod.
	 */
	public static ListMultimap<String, IWDLMessageType> getTypes() {
		ListMultimap<String, IWDLMessageType> returned = LinkedListMultimap.create();
		
		for (MessageRegistration r : registrations) {
			returned.put(r.name, r.type);
		}
		
		return ImmutableListMultimap.copyOf(returned);
	}
	
	/**
	 * Should be called when the server has changed.
	 */
	public static void onNewServer() {
		if (!WDL.baseProps.containsKey("Messages.enableAll")) {
			if (WDL.baseProps.containsKey("Debug.globalDebugEnabled")) {
				//Port from old version.
				WDL.baseProps.put("Messages.enableAll",
						WDL.baseProps.remove("Debug.globalDebugEnabled"));
			} else {
				WDL.baseProps.setProperty("Messages.enableAll", "true");
			}
			enableAllMessages = WDL.baseProps.getProperty("Messages.enableAll")
					.equals("true");
		}
	}
}

/**
 * Enum containing WDL's default {@link IWDLMessageType}s.
 * <br/>
 * Contains a modifiable boolean that states whether the level is
 * enabled.  
 */
enum WDLMessageTypes implements IWDLMessageType {
	LOAD_TILE_ENTITY("Loading TileEntity", false),
	ON_WORLD_LOAD("World loaded", false),
	ON_BLOCK_EVENT("Block Event", true),
	ON_MAP_SAVED("Map data saved", false),
	ON_CHUNK_NO_LONGER_NEEDED("Chunk unloaded", false), 
	ON_GUI_CLOSED_INFO("GUI Closed -- Info", true),
	ON_GUI_CLOSED_WARNING("GUI Closed -- Warning", true),
	SAVING("Saving data", true),
	REMOVE_ENTITY("Removing entity", false),
	PLUGIN_CHANNEL_MESSAGE("Plugin channel message", true);
	
	private WDLMessageTypes(String displayText, boolean enabledByDefault) {
		this.displayText = displayText;
		this.enabledByDefault = enabledByDefault;
		
		WDLMessages.registerMessage(this.name(), this, "Core");
	}
	
	/**
	 * Text to display on a button for this enum value.
	 */
	private final String displayText;
	/**
	 * Whether this type of message is enabled by default.
	 */
	private final boolean enabledByDefault;
	
	public String getDisplayName() {
		return this.displayText;
	}

	@Override
	public String getTitleColor() {
		return "§2";
	}
	
	@Override
	public String getTextColor() {
		return "§6";
	}

	@Override
	public String getDescription() {
		// TODO NYI
		return "";
	}
	
	@Override
	public boolean isEnabledByDefault() {
		return enabledByDefault;
	}
}
