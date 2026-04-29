package tools.refinery.generator.gui;

import tools.refinery.generator.standalone.StandaloneRefinery;

import javax.swing.*;
import java.io.IOException;

/**
 * Main entry point for the Refinery GUI applying the MVC architecture.
 */
public class RefineryGui {

	public static void main(String[] args) throws IOException {
		var problem = StandaloneRefinery.getProblemLoader().loadString("""
				% Metamodel
				class Person {
				    contains Post[] posts opposite author
				    contains Picture[] pictures
				    Person[] friend opposite friend
				    Dog[] pets opposite owner
				}

				class Post {
				    container Person[0..1] author opposite posts
				    Post replyTo
				}

				class Picture.

				class Dog {
					Person owner opposite pets
				}

				% Constraints
				error replyToNotFriend(Post x, Post y) <->
				    replyTo(x, y),
				    author(x, xAuthor),
				    author(y, yAuthor),
				    xAuthor != yAuthor,
				    !friend(xAuthor, yAuthor).

				error replyToCycle(Post x) <-> replyTo+(x, x).

				% Instance model
				!friend(a, b).
				author(p1, a).
				author(p2, b).

				!author(Post::new, a).

				% Scope
				scope Post = 5, Person = 5.
				""");
		var generator =
				StandaloneRefinery.getGeneratorFactory().debugPartialInterpretations(true).createGenerator(problem);

		SwingUtilities.invokeLater(() -> {
			RefineryGuiModel appModel = new RefineryGuiModel();
			RefineryGuiView view = new RefineryGuiView(appModel);
			RefineryGuiController controller = new RefineryGuiController(appModel, generator);
			view.setController(controller);

			view.show();
		});
	}
}
