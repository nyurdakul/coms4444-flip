package flip.g1;
import java.util.*;
import java.util.List;

import javafx.util.Pair;
import java.util.TreeMap;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;

public class Player implements flip.sim.Player
{
	private int seed = 42;
	private Random random;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;
	private Integer threshold = 21;
	private Integer boundary = 20;
	private Integer height_to_count_players = 2;
	private Double distance = 3.73; // ~= 2 + sqrt(3)
	private Double density_cone_height = 4.0;
	private Double density_lane_gap = 1.0;


	public Player()
	{
		random = new Random(seed);
	}

	// Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
	{
		this.n = n;
		this.isplayer1 = isplayer1;
		this.diameter_piece = diameter_piece;
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		int sign = isplayer1 ? -1 : 1;
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
		int low1 = Integer.MAX_VALUE, low2 = Integer.MAX_VALUE, low1Id = -1, low2Id = -1;

		for (int i = 0; i < n; i++) {  
			int count_behind_players = 0;
			Point curr_position = player_pieces.get(i);
			//count the number of nodes incoming
			for (Point point : player_pieces.values()) {
				double y = point.y;
				double x = point.x;
				if (y > curr_position.y - height_to_count_players && y < curr_position.y + height_to_count_players
				&& ((isplayer1 && x > curr_position.x) || (!isplayer1 && x < curr_position.x)) ) 
					count_behind_players++;
			}
			Pair<Integer, Point> move = new Pair<Integer, Point>(i, new Point(curr_position.x + sign*2, curr_position.y));
			//Choose 2.5 not 2 to allow some intersection that stops the coin moving into the stop area.
			if(((isplayer1 && curr_position.x < -threshold - count_behind_players*2.5)
				|| (!isplayer1 && curr_position.x > threshold + count_behind_players*2.5)) ||
					!check_validity(move, player_pieces, opponent_pieces)) continue;
			int count = 0; 
			for (Point point : opponent_pieces.values()) {
				// Two lines parameters
				double k1 = -sign * distance / density_cone_height;
				double b1 = curr_position.y  - k1 * (curr_position.x - sign * density_lane_gap);
				double k2 = sign * distance / density_cone_height;
				double b2 = curr_position.y  - k2 * (curr_position.x - sign * density_lane_gap);
				double x = point.x;
				double y = point.y;
				boolean condition1 = y < (curr_position.y + distance);
				boolean condition2 = y > (curr_position.y - distance);
				boolean condition3 = (x * k1 + b1 - y) < 0;
				boolean condition4 = (x * k2 + b2 - y) > 0;
				if(condition1 && condition2 && condition3 && condition4)
					count++;
			}
			if (low1 > count) {        
				low2 = low1;	
				low2Id = low1Id;
				low1 = count;
				low1Id = i;
			}
			else if (low2 > count) {
				low2 = count;
				low2Id = i;
			}
		}
		if (low1Id != -1) {
			Point point1 = player_pieces.get(low1Id);
			Pair<Integer, Point> move1 = new Pair<Integer, Point>(low1Id, new Point(point1.x + sign*2, point1.y));
			moves.add(move1);
			Pair<Integer, Point> move2 = new Pair<Integer, Point>(low1Id, new Point(point1.x + sign * 4, point1.y));
			//We should check if the current node has got into the stop area, if so, do movement for the other nodes.
			if (check_validity(move2, player_pieces, opponent_pieces) 
				&& ((isplayer1 && point1.x + sign*2 > -threshold) || (!isplayer1 && point1.x + sign*2 < threshold)))
				moves.add(move2);
			else if (low2Id != -1) {
				Point point2 = player_pieces.get(low2Id);
				Pair<Integer, Point> move3 = new Pair<Integer, Point>(low2Id, new Point(point2.x + sign * 2, point2.y));
				moves.add(move3);
			}
		}

		// Attempt brute escape for each node.
		// Potential optimization: prioritize closest stuck nodes or least-density nodes first!
		// Another potential optimization: if it breaks free, prioritize moving the free one over another stuck one.
		for(int id = 0; id<n; id++){
			if(moves.size() == num_moves){
				break;
			}
			Point curr_position = player_pieces.get(id);
			if(((isplayer1 && curr_position.x < -threshold)
					|| (!isplayer1 && curr_position.x > threshold))) continue;
			double theta_up = 0;
			double theta_down = 0;
			double delta_x = 0;
			double delta_y_up = 0;
			double delta_y_down = 0;
			Point new_position_up = new Point(curr_position);
			Point new_position_down = new Point(curr_position);
			if(isplayer1){
				// going left...
				// try up 135 degrees
				theta_up = (3/4)*Math.PI;
				// Try down 225 degrees
				theta_down = (5/4)*Math.PI;

				delta_x = diameter_piece * Math.cos(theta_up);
				delta_y_up = diameter_piece * Math.sin(theta_up);
				delta_y_down = diameter_piece * Math.sin(theta_down);

				new_position_up.x -=delta_x;
				new_position_down.x -=delta_x;

				new_position_up.y += delta_y_up;
				new_position_down.y += delta_y_down;

			}
			else{
				// going right.
				// Try up 45 degrees
				theta_up = (1/4)*Math.PI;
				// Try down 315 degrees
				theta_down = (7/8)*Math.PI;

				delta_x = diameter_piece * Math.cos(theta_up);
				delta_y_up = diameter_piece * Math.sin(theta_up);
				delta_y_down = diameter_piece * Math.sin(theta_down);

				new_position_up.x +=delta_x;
				new_position_down.x +=delta_x;

				new_position_up.y += delta_y_up;
				new_position_down.y += delta_y_down;


			}
			Pair<Integer, Point> move_up = new Pair<Integer, Point>(id, new_position_up);
			Pair<Integer, Point> move_down = new Pair<Integer, Point>(id, new_position_down);


			if(check_validity(move_up, player_pieces, opponent_pieces))
				moves.add(move_up);
			else if(check_validity(move_down, player_pieces, opponent_pieces))
				moves.add(move_down);
		}
		
		int num_trials = 30;
		int i = 0;
    
		while(moves.size()!= num_moves && i<num_trials)  {
			Integer piece_id = random.nextInt(n);
			
		 	Point curr_position = player_pieces.get(piece_id);
			if(((isplayer1 && curr_position.x < -threshold)
				|| (!isplayer1 && curr_position.x > threshold))) continue;
			Point new_position = new Point(curr_position);
		 	double theta = -Math.PI/2 + Math.PI * random.nextDouble();
		 	double delta_x = diameter_piece * Math.cos(theta);
		 	double delta_y = diameter_piece * Math.sin(theta);

		 	Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
		 	// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
		 	// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

		 	new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
		 	new_position.y += delta_y;
		 	Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);

		 	Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
		 	// System.out.println("distance from previous position is " + dist.toString());
		 	// Log.record("distance from previous position is " + dist.toString());

		 	if(check_validity(move, player_pieces, opponent_pieces))
		 		moves.add(move);
		 	i++;
		 }
		 return moves;
	}

	public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        boolean valid = true;
       
        // check if move is adjacent to previous position.
        if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), diameter_piece))
            {
                return false;
            }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;
    }

}

