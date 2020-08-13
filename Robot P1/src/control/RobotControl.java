package control;

import robot.Robot;

//Robot Assignment for Programming 1 s2 2020
//Adapted by Caspar and Ross from original Robot code written by Dr Charles Thevathayan
public class RobotControl implements Control {
	// we need to internally track where the arm is
	private int height = Control.INITIAL_HEIGHT;
	private int width = Control.INITIAL_WIDTH;
	private int depth = Control.INITIAL_DEPTH;

	private int[] barHeights;
	private int[] blockHeights;
	private int[] columnHeights;
	private int[] rowOne;
	private int[] rowTwo;
	private int[] rowThree;
	private int leftPickStack;
	private int rightPickStack;

	private Robot robot;

	// called by RobotImpl
	@Override
	public void control(Robot robot, int[] barHeightsDefault, int[] blockHeightsDefault) {
		this.robot = robot;

		// some hard coded init values you can change these for testing
		//this.barHeights = new int[] { 0, 0, 1, 3, 0, 2, 1 };
		//this.blockHeights = new int[] { 3, 1, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1};

		// FOR SUBMISSION: uncomment following 2 lines
		this.barHeights = barHeightsDefault;
		this.blockHeights = blockHeightsDefault;

		// initialise the robot
		robot.init(this.barHeights, this.blockHeights, height, width, depth);

		// ADD ASSIGNMENT PART A METHOD CALL(S) HERE
		int numberOfBlocks = this.blockHeights.length;
		int[] orderOfBlockHeights = new int[numberOfBlocks];
		int stackOne = Control.STACK1_COLUMN; // 1
		int stackTwo = Control.STACK2_COLUMN; // 10
		this.leftPickStack = Control.STACK1_COLUMN - 1; // 1** -1 for array position
		this.rightPickStack = Control.STACK2_COLUMN - 1; // 10** -1 for array position

		getOrderOfBlockHeights(numberOfBlocks, orderOfBlockHeights);
		findStackHeights(numberOfBlocks, stackOne, stackTwo);

		picker(orderOfBlockHeights, numberOfBlocks);
	}

	private void picker(int[] orderOfBlockHeights, int numberOfBlocks) {
		int currentBlock = 0;
		int pickLocation = Control.FIRST_BAR_COLUMN - 1; // start here for picking of the first block (2) -1 for array
															// position

		int currentDropDestination = this.leftPickStack; // need to change this after block is dropped

		while (currentBlock < numberOfBlocks) {
			int lowestPoint;

			while (pickLocation < rightPickStack && currentBlock < numberOfBlocks) {
				pickBlock(orderOfBlockHeights, currentBlock, pickLocation);

				dropBlock(orderOfBlockHeights, currentBlock, currentDropDestination);
				
				currentDropDestination = currentDropDestination == this.leftPickStack ? this.rightPickStack
						: this.leftPickStack;
				if(pickLocation == 8) {
					currentBlock++;
					break;
				}
				pickLocation++;
				currentBlock++;
			}
			while(pickLocation > leftPickStack && currentBlock < numberOfBlocks) {
				pickBlock(orderOfBlockHeights, currentBlock, pickLocation);

				dropBlock(orderOfBlockHeights, currentBlock, currentDropDestination);
				
				currentDropDestination = currentDropDestination == this.leftPickStack ? this.rightPickStack
						: this.leftPickStack;
				if(pickLocation == 1) {
					currentBlock++;
					break;
				}
				pickLocation--;
				currentBlock++;
			}
		}
		while(this.width > Control.MIN_WIDTH) {
			robot.contract();
			width--;
		}
	}

	private void dropBlock(int[] orderOfBlockHeights, int currentBlock, int currentDropDestination) {
		int lowestPoint;
		lowestPoint = findCarryHeight(currentDropDestination, currentBlock, orderOfBlockHeights);
		reachDropHeight(lowestPoint);
		moveToDropLocation(currentDropDestination, currentBlock);
		lowerAndDrop(orderOfBlockHeights, currentDropDestination, currentBlock);
	}

	private void pickBlock(int[] orderOfBlockHeights, int currentBlock, int pickLocation) {
		int lowestPoint;
		lowestPoint = getLowestToPickPoint(pickLocation);
		reachPickHeight(lowestPoint);
		moveToPickPosition(pickLocation);
		lowerAndPick(pickLocation);
		removeBlockHeight(pickLocation, currentBlock, orderOfBlockHeights);
	}

	private void lowerAndDrop(int[] orderOfBlockHeights, int currentDropDestination, int currentBlock) {
		int dropStackHeight = currentDropDestination == this.leftPickStack ? this.columnHeights[0]
				: this.columnHeights[9];
		int originalDepth = this.depth;
		while(this.depth + dropStackHeight + orderOfBlockHeights[currentBlock] < (this.height - 1)) {
			robot.lower();
			this.depth++;
		}
		robot.drop();
		if (currentDropDestination == this.leftPickStack) {
			this.columnHeights[0] += orderOfBlockHeights[currentBlock];
		} else {
			this.columnHeights[9] += orderOfBlockHeights[currentBlock];
		}
		
		while(this.depth > originalDepth) {
			robot.raise();
			this.depth--;
		}
	}

	private void moveToDropLocation(int currentDropDestination, int currentBlock) {
		if (currentDropDestination == this.leftPickStack) {
			while (this.width > currentDropDestination + 1) { // +1 for correct comparison with width value
				robot.contract();
				this.width--;
			}
		} else {
			while (this.width < currentDropDestination + 1) { // +1 for correct comparison with width value
				robot.extend();
				this.width++;
			}
		}
	}

	private void reachDropHeight(int lowestPoint) {
		while (this.height > lowestPoint) {
			robot.down();
			this.height--;
		}
		while (this.height < lowestPoint) {
			robot.up();
			this.height++;
		}
	}

	private int findCarryHeight(int currentDropDestination, int currentBlock, int[] orderOfBlockHeights) {
		int lowestPoint = 0;
		if (currentDropDestination == this.leftPickStack) {
			int i = this.width - 1; // -1 for correct position in array
			while (i >= currentDropDestination) {
				if (this.columnHeights[i] > lowestPoint) {
					lowestPoint = this.columnHeights[i];
				}
				i--;
			}

		} else {
			int lowestBeforePickPosition = 0;
			int lowestAfterPickPosition = 0;
			int i = this.width - 1; // -1 for correct position in array
			int currentPosition = this.width - 1;
			while(i <= currentDropDestination) {
				if (this.columnHeights[i] > lowestAfterPickPosition) {
					lowestAfterPickPosition = this.columnHeights[i];
					lowestPoint = lowestAfterPickPosition;
				}
				i++;
			}
			i = 0;
			while(i < currentPosition) {
				if (this.columnHeights[i] > lowestBeforePickPosition) {
					lowestBeforePickPosition = this.columnHeights[i];
				}
				i++;
			}
			
			if(lowestAfterPickPosition > this.height + orderOfBlockHeights[currentBlock] + 1) {
				lowestPoint = lowestAfterPickPosition;
			}
			if (lowestBeforePickPosition > lowestPoint + orderOfBlockHeights[currentBlock]) {
				lowestPoint = lowestBeforePickPosition;
				return lowestPoint + 1;
			}
		}
		return lowestPoint + orderOfBlockHeights[currentBlock] + 1;
	}

	private void removeBlockHeight(int pickLocation, int currentBlock, int[] orderOfBlockHeights) {
		this.columnHeights[pickLocation] -= orderOfBlockHeights[currentBlock];
	}

	private void lowerAndPick(int pickLocation) {
		int originalDepth = this.depth;
		while ((this.depth + this.columnHeights[pickLocation]) < (this.height - 1)) {
			robot.lower();
			depth++;
		}
		robot.pick();
		while (this.depth > originalDepth) {
			robot.raise();
			depth--;
		}
	}

	private void moveToPickPosition(int pickLocation) {
		while (this.width < pickLocation + 1) { // +1 for correct position on the columns
			robot.extend();
			this.width++;
		}
		while(this.width > pickLocation + 1) { // +1 for correct position on the columns
			robot.contract();
			this.width--;
		}
	}

	private void reachPickHeight(int lowestPoint) {
		while (this.height > lowestPoint + 1) { // + 1 for clearence height of the arm. otherwise would be even
												// and clash
			robot.down();
			this.height--;
		}
		while(this.height < lowestPoint + 1) {
			robot.up();
			this.height++;
		}
	}

	private int getLowestToPickPoint(int pickLocation) {
		int lowestPickPoint = 0;
		int currentLocation = this.width - 1; // -1 for correct position in array

		int i = currentLocation;
		if(currentLocation < pickLocation) { 
			while (i <= pickLocation) {
				if (lowestPickPoint < this.columnHeights[i]) {
					lowestPickPoint = this.columnHeights[i];
				}
				i++;
			}
		} else {
			while (i >= 0) { 
				if (lowestPickPoint < this.columnHeights[i]) {
					lowestPickPoint = this.columnHeights[i];
				}
				i--;
			}
			if(this.columnHeights[0] > lowestPickPoint) {
				lowestPickPoint = this.columnHeights[0];
			}
		}
		
		return lowestPickPoint;
	}

	private void findStackHeights(int numberOfBlocks, int stackOne, int stackTwo) {
		this.columnHeights = new int[10];

		// cycles through each of the bars and adds them to the height of the columns
		// has the length of the bar heights array to stop it and in case of removal or
		// addition of blocks
		int i = stackOne; // start at one to skip the left stack column
		int x = 0;
		while (x < this.barHeights.length) {
			this.columnHeights[i] = this.barHeights[x];
			i++;
			x++;
		}

		// while loop to drop blocks up the columns and then a while to drop down the
		// columns
		i = stackOne - 1; // -1 for correct position in array
		x = stackTwo - 1; // -1 for correct position in array
		int currentColumn = 1; // initialize at one for proper array placement in column heights
		int y = 0;
		while (y < numberOfBlocks) {
			while (currentColumn < stackTwo - 1 && y < numberOfBlocks) {
				this.columnHeights[currentColumn] += this.blockHeights[y];
				currentColumn++;
				y++;
			}
			currentColumn = 8;
			while (currentColumn > i && y < numberOfBlocks) {
				this.columnHeights[currentColumn] += this.blockHeights[y];
				currentColumn--;
				y++;
			}
			currentColumn = 1; // reset back to one for any additional blocks
		}

	}

	// finds the height of the blocks in the order that they will be picked
	// three different algorithms for if their are >8, >8 but <16, and if there are
	// >16. Don't need anymore
	// than this as 24 is the most block you can have the last algorithms takes care
	// of this.
	private void getOrderOfBlockHeights(int numberOfBlocks, int[] orderOfBlockHeights) {
		int numberOfColumns = Control.MAX_WIDTH - 2; // minus 2 for the two stacking columns
		int rowTwoBlocksNumber = 0;
		int rowThreeBlocksNumber = 0;

		rowOne = new int[numberOfColumns];
		if ((numberOfBlocks > numberOfColumns) && numberOfBlocks <= (numberOfColumns * 2)) {
			rowTwoBlocksNumber = numberOfColumns - ((numberOfColumns * 2) % numberOfBlocks);
		} else if (numberOfBlocks > numberOfColumns * 2) {
			rowTwoBlocksNumber = numberOfColumns;
			rowThreeBlocksNumber = numberOfColumns - (numberOfColumns * 3) % numberOfBlocks;
		}
		rowTwo = new int[rowTwoBlocksNumber];
		rowThree = new int[rowThreeBlocksNumber];

		if (numberOfBlocks <= numberOfColumns) {
			for (int i = 0; i < numberOfBlocks; i++) {
				orderOfBlockHeights[i] = this.blockHeights[i];
			}
		} else if ((numberOfBlocks > numberOfColumns) && numberOfBlocks <= (numberOfColumns * 2)) {

			int i = 0;
			while (i < numberOfColumns) {
				rowOne[i] = this.blockHeights[i];
				i++;
			}
			int x = 0;
			while (i < numberOfBlocks) {
				rowTwo[x] = this.blockHeights[i];
				x++;
				i++;
			}

			twoRowBlockHeightPicks(numberOfBlocks, orderOfBlockHeights, numberOfColumns, rowTwoBlocksNumber);

		} else {
			threeRowBlockHeightPicks(numberOfBlocks, orderOfBlockHeights, numberOfColumns);
		}
	}

	private void twoRowBlockHeightPicks(int numberOfBlocks, int[] orderOfBlockHeights, int numberOfColumns,
			int rowTwoBlocksNumber) {
		int i;
		int x;
		i = 0;
		while (i < numberOfBlocks) {
			while (i < numberOfColumns - rowTwoBlocksNumber) {
				orderOfBlockHeights[i] = rowOne[i];
				i++;
			}
			x = rowTwoBlocksNumber - 1;
			while (x >= 0) {
				orderOfBlockHeights[i] = rowTwo[x];
				i++;
				x--;
			}
			x = rowOne.length - 1;
			while (x >= rowOne.length - rowTwoBlocksNumber) {
				orderOfBlockHeights[i] = rowOne[x];
				i++;
				x--;
			}
			i++;
		}
	}

	// try to make a method out of the while loops in here
	private void threeRowBlockHeightPicks(int numberOfBlocks, int[] orderOfBlockHeights, int numberOfColumns) {
		int i = 0;
		while (i < numberOfColumns) {
			rowOne[i] = this.blockHeights[i];
			i++;
		}
		int x = 7;
		while (x >= 0) {
			rowTwo[x] = this.blockHeights[i];
			x--;
			i++;
		}
		int y = 0;
		while (i < numberOfBlocks) {
			rowThree[y] = this.blockHeights[i];
			y++;
			i++;
		}

		i = 0;
		while (i < rowThree.length) {
			orderOfBlockHeights[i] = rowThree[i];
			i++;
		}

		x = rowThree.length;
		while (x < rowTwo.length) {
			orderOfBlockHeights[i] = rowTwo[x];
			x++;
			i++;
		}

		x = rowOne.length - 1;
		while (x > rowThree.length - 1) {
			orderOfBlockHeights[i] = rowOne[x];
			x--;
			i++;
		}

		x = rowThree.length - 1;
		while (x >= 0) {
			orderOfBlockHeights[i] = rowTwo[x];
			x--;
			i++;
		}

		x = 0;
		while (x < rowThree.length) {
			orderOfBlockHeights[i] = rowOne[x];
			x++;
			i++;
		}
	}

}
