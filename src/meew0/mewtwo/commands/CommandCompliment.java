package meew0.mewtwo.commands;

import meew0.mewtwo.MewtwoListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

public class CommandCompliment implements ICommand {

	@Override
	public String getCommandName() {
		return "compliment";
	}

	@Override
	public void onExecution(String[] args, MessageEvent<PircBotX> event) {
		String compliment = "";
		if (args.length > 0)
			compliment += (args[0] + ", ");
		
		String c2 = "";
		try {
			Document doc = Jsoup.connect("http://www.chainofgood.co.uk/passiton")
					.get();
			Elements medium = doc.select(".medium");
			
			c2 = medium.get(MewtwoListener.rnd.nextInt(medium.size()))
					.toString().replaceAll("<[^>]*>", "");
		} catch(Exception e) {
			throw new RuntimeException("Exception while getting a compliment! :(", e);
		} finally {
			if(c2.equals("")) c2 = "I'm glad you exist";
		}
		
		compliment += c2;
		
		event.getChannel().send().message(compliment);
	}

	@Override
	public String getHelpEntry() {
		return "compliment|c [person]: Compliments the whole channel (or the specified person) with a random compliment.";
	}

	@Override
	public String getAlias() {
		return "c";
	}

}