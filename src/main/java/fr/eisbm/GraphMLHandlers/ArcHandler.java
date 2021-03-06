package fr.eisbm.GraphMLHandlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.sbgn.bindings.Arc;
import org.sbgn.bindings.Arc.Next;
import org.sbgn.bindings.Arc.Start;
import org.sbgn.bindings.Arc.End;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Label;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Port;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.eisbm.GRAPHML2SBGNML.ConverterDefines;
import fr.eisbm.GRAPHML2SBGNML.Utils;
import fr.eisbm.GraphMLHandlers.PortArcsRelationship.PortType;

public class ArcHandler {

	java.util.Map<String, PortArcsRelationship> port_arc_map = new HashMap<String, PortArcsRelationship>();
	java.util.Map<String, HashSet<Arc>> glyph_arc_map = new HashMap<String, HashSet<Arc>>();
	private Set<String> reversibleSet = new HashSet<String>();

	public void processArcs(NodeList nEdgeList, Map map, StyleHandler sh) {
		for (int temp = 0; temp < nEdgeList.getLength(); temp++) {
			Node nEdge = nEdgeList.item(temp);
			Arc arc = new Arc();

			if (nEdge.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nEdge;

				// get id of the edge/arc
				String szArcAttributes = getElementAttributes(eElement).trim();
				String szArrowDirection = processNodeList(eElement.getElementsByTagName(ConverterDefines.Y_ARROWS));
				setArcSourceTarget(arc, szArcAttributes, szArrowDirection, map);

				// given the fact that the consumption line could be used also for the
				// representation of the logic or equivalence arcs, this has to be checked and
				// decoded accordingly
				correctArcClazz(arc);

				// set coordinates for Start/End fields of the arc
				String szPathCoordinates = processNodeList(eElement.getElementsByTagName(ConverterDefines.Y_PATH));
				setArcStartEnd(arc, szPathCoordinates);

				// set bend points of the arc
				String szPointInfo = processNodeList(eElement.getElementsByTagName(ConverterDefines.Y_POINT));
				setBendPoints(arc, szPointInfo);

				// set arc style (color, line width etc)
				NodeList nlLineStyle = eElement.getElementsByTagName(ConverterDefines.Y_LINE_STYLE);
				setArcStyle(eElement, nlLineStyle, sh);

				// set cardinality of the arc
				NodeList nlCardinalityList = eElement.getElementsByTagName(ConverterDefines.Y_EDGE_LABEL);
				setArcCardinality(arc, nlCardinalityList);
			}

			// add the arc to the map
			map.getArc().add(arc);
		}
	}

	public String processNodeList(NodeList nodeList) {
		String szContent = "";
		for (int temp = 0; temp < nodeList.getLength(); temp++) {
			Node nNode = nodeList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				szContent = szContent.concat(getElementAttributes(eElement));
			}
		}
		return szContent;
	}

	public String getElementAttributes(Element eElement) {
		String szAttributeValues = "";
		for (int i = 0; i < eElement.getAttributes().getLength(); i++) {
			szAttributeValues = szAttributeValues.concat(eElement.getAttributes().item(i) + "\t");
		}
		return szAttributeValues;
	}

	private void correctArcClazz(Arc arc) {
		if (arc.getClazz().equals(ConverterDefines.SBGN_CONSUMPTION)) {
			if ((arc.getSource() != null) && (arc.getTarget() != null)) {

				if (((Glyph) arc.getSource()).getClazz().toUpperCase().equals("OR")
						|| ((Glyph) arc.getTarget()).getClazz().toUpperCase().equals("OR")
						|| (((Glyph) arc.getSource()).getClazz().toUpperCase().equals("AND")
								|| ((Glyph) arc.getTarget()).getClazz().toUpperCase().equals("AND"))
						|| (((Glyph) arc.getSource()).getClazz().toUpperCase().equals("NOT")
								|| ((Glyph) arc.getTarget()).getClazz().toUpperCase().equals("NOT"))) {
					arc.setClazz(ConverterDefines.SBGN_LOGIC_ARC);
				} else if ((((Glyph) arc.getSource()).getClazz().equals(ConverterDefines.SBGN_SUBMAP))
						|| ((Glyph) arc.getSource()).getClazz().equals(ConverterDefines.SBGN_TAG)
						|| ((Glyph) arc.getTarget()).getClazz().equals(ConverterDefines.SBGN_SUBMAP)
						|| ((Glyph) arc.getTarget()).getClazz().equals(ConverterDefines.SBGN_TAG))
					arc.setClazz(ConverterDefines.SBGN_EQUIVALENCE_ARC);
			}
		}
	}

	private void setArcStartEnd(Arc arc, String szPathCoordinates) {
		float fStartX = 0, fStartY = 0, fStartH = 0, fStartW = 0, fEndX = 0, fEndY = 0, fEndH = 0, fEndW = 0;
		Glyph source = (Glyph) arc.getSource();
		Glyph target = (Glyph) arc.getTarget();

		if (null != source) {
			if (null != source.getBbox()) {
				fStartX = source.getBbox().getX();
				fStartY = source.getBbox().getY();
				fStartH = source.getBbox().getH();
				fStartW = source.getBbox().getW();
			}
		}

		if (null != target) {
			if (null != target.getBbox()) {
				fEndX = target.getBbox().getX();
				fEndY = target.getBbox().getY();
				fEndH = target.getBbox().getH();
				fEndW = target.getBbox().getW();
			}
		}

		String delimsCoord = "[\t]";
		szPathCoordinates = szPathCoordinates.replaceAll("\"", "");
		String[] tokensCoordinates = szPathCoordinates.split(delimsCoord);
		if (tokensCoordinates.length == 4) {
			String sx = tokensCoordinates[0].replaceAll("sx=", "");
			float valueSX = Float.parseFloat(sx);
			valueSX = valueSX + fStartX + fStartW / 2;

			String sy = tokensCoordinates[1].replaceAll("sy=", "");
			float valueSY = Float.parseFloat(sy);
			valueSY = valueSY + fStartY + fStartH / 2;

			Start start = new Start();
			start.setX(valueSX);
			start.setY(valueSY);
			arc.setStart(start);

			String tx = tokensCoordinates[2].replaceAll("tx=", "");
			float valueTX = Float.parseFloat(tx);
			valueTX = valueTX + fEndX + fEndW / 2;

			String ty = tokensCoordinates[3].replaceAll("ty=", "");
			float valueTY = Float.parseFloat(ty);
			valueTY = valueTY + fEndY + fEndH / 2;

			End end = new End();
			end.setX(valueTX);
			end.setY(valueTY);
			arc.setEnd(end);
		}
	}

	public void setArcStyle(Element eElement, NodeList nlLineStyle, StyleHandler sh) {
		// getting the border color info
		String szStrokeColorId = ((Element) (nlLineStyle.item(0))).getAttribute(ConverterDefines.COLOR_ATTR);
		sh.colorSet.add(szStrokeColorId);

		// getting the stroke width info
		float fStrokeWidth = Float
				.parseFloat(((Element) (nlLineStyle.item(0))).getAttribute(ConverterDefines.WIDTH_ATTR));

		String szStyleId = ConverterDefines.STYLE_PREFIX + fStrokeWidth + szStrokeColorId.replaceFirst("#", "");

		if (!sh.styleMap.containsKey(szStyleId)) {
			sh.styleMap.put(szStyleId, new SBGNMLStyle(szStyleId, szStrokeColorId, fStrokeWidth));
		}
		sh.styleMap.get(szStyleId).addElementIdToSet(eElement.getAttribute(ConverterDefines.ID_ATTR));
	}

	public void setArcCardinality(Arc arc, NodeList nlCardinalityList) {
		if (nlCardinalityList.getLength() > 0) {
			String szCardinality = nlCardinalityList.item(0).getTextContent().trim();
			if (!szCardinality.equals("")) {

				Glyph cardGlyph = new Glyph();
				cardGlyph.setClazz(ConverterDefines.SBGN_CARDINALITY);
				cardGlyph.setId(ConverterDefines.SBGN_CARDINALITY + "_" + szCardinality);
				Label _label = new Label();
				_label.setText(szCardinality);
				cardGlyph.setLabel(_label);
				Bbox cardBbox = new Bbox();
				cardBbox.setH(0);
				cardBbox.setW(0);
				cardBbox.setX(0);
				cardBbox.setY(0);
				cardGlyph.setBbox(cardBbox);
				arc.getGlyph().add(cardGlyph);
			}
		}
	}

	public void setBendPoints(Arc arc, String szPointInfo) {
		if (!szPointInfo.isEmpty()) {

			String delims = "[\t]";
			float fXCoord = 0, fYCoord = 0;
			szPointInfo = szPointInfo.replaceAll("\"", "");
			String[] tokensBendPoint = szPointInfo.split(delims);

			for (int i = 0; i < tokensBendPoint.length - 1; i += 2) {
				if (tokensBendPoint[i].contains("x=")) {
					fXCoord = Float.parseFloat(tokensBendPoint[i].replaceAll("x=", ""));
				}
				if (tokensBendPoint[i + 1].contains("y=")) {
					fYCoord = Float.parseFloat(tokensBendPoint[i + 1].replaceAll("y=", ""));
				}

				boolean bFoundPort = false;
				if (arc.getSource() instanceof Glyph) {
					Glyph g = (Glyph) arc.getSource();
					bFoundPort = isPortPoint(fXCoord, fYCoord, g);
				}

				if (!bFoundPort) {
					if (arc.getTarget() instanceof Glyph) {
						Glyph g = (Glyph) arc.getTarget();
						bFoundPort = isPortPoint(fXCoord, fYCoord, g);
					}
				}

				if (!bFoundPort) {

					boolean bBendPointFound = false;
					for (Next n : arc.getNext()) {
						if ((n.getX() == fXCoord) && (n.getY() == fYCoord)) {
							bBendPointFound = true;
							break;
						}
					}

					if (!bBendPointFound) {
						Next next = new Next();
						next.setX(fXCoord);
						next.setY(fYCoord);
						arc.getNext().add(next);
					}
				} 
			}
		}
	}

	private boolean isPortPoint(float fXCoord, float fYCoord, Glyph g) {
		boolean bFoundPort = false;
		if (Utils.isProcessType(g) || Utils.isOperatorType(g)) {
			float fCenterGlyphX = (float) (g.getBbox().getX() + g.getBbox().getW() * 0.5);
			float fCenterGlyphY = (float) (g.getBbox().getY() + g.getBbox().getH() * 0.5);
			float dist = Utils.getPointDistance(fCenterGlyphX, fCenterGlyphY, fXCoord, fYCoord);
			float fPort2GlyphDistance = g.getBbox().getW();

			if (dist <= fPort2GlyphDistance) {
				bFoundPort = true;
			}
		}
		return bFoundPort;
	}

	public boolean setArcClazz(Arc arc, String szArrowDirection) {
		String szArcType = ConverterDefines.SBGN_CONSUMPTION;
		boolean bEdgeToBeCorrected = false;

		if (szArrowDirection.contains("white_delta_bar")) {
			szArcType = ConverterDefines.SBGN_NECESSARY_STIMULATION;
			if (szArrowDirection.contains("source=\"white_delta_bar\"")) {
				bEdgeToBeCorrected = true;
			}
		} else if (szArrowDirection.contains("white_diamond")) {
			szArcType = ConverterDefines.SBGN_MODULATION;
			if (szArrowDirection.contains("source=\"white_diamond\"")) {
				bEdgeToBeCorrected = true;
			}
		} else if (szArrowDirection.contains("t_shape")) {
			szArcType = ConverterDefines.SBGN_INHIBITION;
			if (szArrowDirection.contains("source=\"t_shape\"")) {
				bEdgeToBeCorrected = true;
			}
		} else if (szArrowDirection.contains("white_delta")) {
			szArcType = ConverterDefines.SBGN_STIMULATION;
			if (szArrowDirection.contains("source=\"white_circle\"")) {
				bEdgeToBeCorrected = true;
			}
		} else if (szArrowDirection.contains("delta")) {
			szArcType = ConverterDefines.SBGN_PRODUCTION;
			if (szArrowDirection.contains("source=\"delta\"")) {
				bEdgeToBeCorrected = true;
			}
		} else if (szArrowDirection.contains("white_circle")) {
			szArcType = ConverterDefines.SBGN_CATALYSIS;
			if (szArrowDirection.contains("source=\"white_circle\"")) {
				bEdgeToBeCorrected = true;
			}
		}

		arc.setClazz(szArcType);
		return bEdgeToBeCorrected;
	}

	public void setArcSourceTarget(Arc arc, String szArcAttributes, String szArrowDirection, Map map) {
		boolean bEdgeToBeCorrected = setArcClazz(arc, szArrowDirection);

		String delims = "[\t]";
		String szArcId = "", szArcSource = "", szArcTarget = "";
		szArcAttributes = szArcAttributes.replaceAll("\"", "");
		String[] tokens = szArcAttributes.split(delims);

		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].contains("id=")) {
				szArcId = tokens[i].replaceAll("id=", "");
				arc.setId(szArcId);
			} else if (tokens[i].contains("source=")) {
				szArcSource = tokens[i].replaceAll("source=", "");

				Glyph g = null;
				for (Glyph _glyph : map.getGlyph()) {
					g = Utils.findGlyph(szArcSource, _glyph);
					if (null != g) {
						arc.setSource(g);
						break;
					}
				}

				if (null == g) {
					System.out.println("source " + szArcSource);
				}
			} else if (tokens[i].contains("target=")) {
				szArcTarget = tokens[i].replaceAll("target=", "");

				Glyph g = null;
				for (Glyph _glyph : map.getGlyph()) {
					g = Utils.findGlyph(szArcTarget, _glyph);
					if (null != g) {
						arc.setTarget(g);
						break;
					}
				}

				if (null == g) {
					System.out.println("target " + szArcTarget);
				}
			}
		}

		Glyph source = (Glyph) arc.getSource();
		Glyph target = (Glyph) arc.getTarget();

		if (bEdgeToBeCorrected) {
			arc.setSource(target);
			arc.setTarget(source);
		}

		// if the process is a consumption, it is easy to draw from process to entity
		// and this can not be detected before
		// given that at this stage the logic arc is drawn using the consumption line,
		// it is needed to check if the line was correctly drawn between the operator
		// and the EPN
		if (arc.getClazz().equals(ConverterDefines.SBGN_CONSUMPTION)) {

			source = (Glyph) arc.getSource();
			target = (Glyph) arc.getTarget();

			if (source != null && target != null) {

				if ((Utils.isProcessType(source)) && (!Utils.isProcessType(target))) {
					arc.setSource(target);
					arc.setTarget(source);
				}
				if ((Utils.isOperatorType(source)) && (!Utils.isOperatorType(target))) {
					arc.setSource(target);
					arc.setTarget(source);
				}
			}
		}
	}

	public void correctPortOrientationAndConnectedArcs(List<Glyph> listGlyphs, List<Arc> listArcs) {
		correctPortOrientation(listGlyphs, listArcs);
		createGlyphArcMap(listGlyphs, listArcs);
		correctConnectedArcs(listGlyphs, listArcs);
	}

	private void correctPortOrientation(List<Glyph> listGlyphs, List<Arc> listArcs) {
		for (Glyph glyph : listGlyphs) {
			int horizontal = 0;
			int vertical = 0;

			if (Utils.isProcessType(glyph)) {

				for (Arc arc : listArcs) {
					boolean bArcAttachedToProcess = isArcConnectedToGlyph(arc, glyph);
					if (bArcAttachedToProcess) {

						if (arc.getClazz().equals(ConverterDefines.SBGN_CONSUMPTION)
								|| arc.getClazz().equals(ConverterDefines.SBGN_PRODUCTION)) {

							LINE_POSITION eArcPosition = getLinePosition(glyph, arc);
							if (eArcPosition.equals(LINE_POSITION.eHorizontal)) {
								horizontal++;
							} else if (eArcPosition.equals(LINE_POSITION.eVertical)) {
								vertical++;
							}
						}
					}
				}

				rearrangePorts(glyph, horizontal, vertical);

			} else if (Utils.isOperatorType(glyph)) {
				for (Arc arc : listArcs) {
					boolean bArcAttachedToProcess = isArcConnectedToGlyph(arc, glyph);

					if (bArcAttachedToProcess) {
						if (arc.getClazz().equals(ConverterDefines.SBGN_LOGIC_ARC)
								|| arc.getClazz().equals(ConverterDefines.SBGN_CATALYSIS)
								|| arc.getClazz().equals(ConverterDefines.SBGN_STIMULATION)
								|| arc.getClazz().equals(ConverterDefines.SBGN_INHIBITION)
								|| arc.getClazz().equals(ConverterDefines.SBGN_MODULATION)
								|| arc.getClazz().equals(ConverterDefines.SBGN_NECESSARY_STIMULATION)) {

							LINE_POSITION eArcPosition = getLinePosition(glyph, arc);
							if (eArcPosition.equals(LINE_POSITION.eHorizontal)) {
								horizontal++;
							} else if (eArcPosition.equals(LINE_POSITION.eVertical)) {
								vertical++;
							}
						}
					}
				}

				rearrangePorts(glyph, horizontal, vertical);
			}

			correctPortOrientation(glyph.getGlyph(), listArcs);
		}
	}

	private void rearrangePorts(Glyph glyph, int horizontal, int vertical) {
		if (horizontal > vertical) {
			glyph.getPort().get(Utils.FIRST_PORT).setX((float) (glyph.getBbox().getX() - glyph.getBbox().getW() * 0.5));
			glyph.getPort().get(Utils.SECOND_PORT)
					.setX((float) (glyph.getBbox().getX() + glyph.getBbox().getW() + glyph.getBbox().getW() * 0.5));

			glyph.getPort().get(Utils.FIRST_PORT).setY((float) (glyph.getBbox().getY() + glyph.getBbox().getH() * 0.5));
			glyph.getPort().get(Utils.SECOND_PORT)
					.setY((float) (glyph.getBbox().getY() + glyph.getBbox().getH() * 0.5));
			if (Utils.isProcessType(glyph)) {
				glyph.setOrientation("horizontal");
			}
		} else if (horizontal < vertical) {
			glyph.getPort().get(Utils.FIRST_PORT).setX((float) (glyph.getBbox().getX() + glyph.getBbox().getW() * 0.5));
			glyph.getPort().get(Utils.SECOND_PORT)
					.setX((float) (glyph.getBbox().getX() + glyph.getBbox().getW() * 0.5));

			glyph.getPort().get(Utils.FIRST_PORT)
					.setY((float) (glyph.getBbox().getY() + glyph.getBbox().getH() + glyph.getBbox().getH() * 0.5));
			glyph.getPort().get(Utils.SECOND_PORT)
					.setY((float) (glyph.getBbox().getY() - glyph.getBbox().getH() * 0.5));
			if (Utils.isProcessType(glyph)) {
				glyph.setOrientation("vertical");
			}
		}
	}

	private void createGlyphArcMap(List<Glyph> listGlyphs, List<Arc> listArcs) {
		for (Glyph glyph : listGlyphs) {
			if (Utils.isProcessType(glyph) || Utils.isOperatorType(glyph)) {
				for (Arc arc : listArcs) {

					if (isArcConnectedToGlyph(arc, glyph)) {
						if (!glyph_arc_map.containsKey(glyph.getId())) {
							glyph_arc_map.put(glyph.getId(), new HashSet<Arc>());
						}
						glyph_arc_map.get(glyph.getId()).add(arc);
					}
				}
			}
			createGlyphArcMap(glyph.getGlyph(), listArcs);
		}
	}

	private void correctConnectedArcs(List<Glyph> listGlyphs, List<Arc> listArcs) {
		for (Glyph glyph : listGlyphs) {
			if (glyph_arc_map.containsKey(glyph.getId())) {
				int nTotalArcNo = glyph_arc_map.get(glyph.getId()).size();
				int nArcsAssignedCorrectly = 0;
				boolean bCorrectlyAssigned = true;
				Port port1 = glyph.getPort().get(Utils.FIRST_PORT);
				Port port2 = glyph.getPort().get(Utils.SECOND_PORT);

				for (Arc arc : glyph_arc_map.get(glyph.getId())) {
					Port currentPort = null;
					Port correctPort = null;
					if (arc.getSource() instanceof Port) {
						currentPort = (Port) arc.getSource();
					} else if (arc.getTarget() instanceof Port) {
						currentPort = (Port) arc.getTarget();
					}
					if (calculateDistance(arc, port1) <= calculateDistance(arc, port2)) {
						correctPort = port1;
					} else {
						correctPort = port2;
					}

					if (correctPort == currentPort) {
						nArcsAssignedCorrectly++;
					}
				}

				if (nArcsAssignedCorrectly < nTotalArcNo * 0.5) {
					bCorrectlyAssigned = false;
				}

				if (!bCorrectlyAssigned) {
					for (Arc arc : glyph_arc_map.get(glyph.getId())) {
						if (arc.getSource() instanceof Port) {
							if (port1 == (Port) arc.getSource()) {
								arc.setSource(port2);
							} else if (port2 == (Port) arc.getSource()) {
								arc.setSource(port1);
							}
						} else if (arc.getTarget() instanceof Port) {
							if (port1 == (Port) arc.getTarget()) {
								arc.setTarget(port2);
							} else if (port2 == (Port) arc.getTarget()) {
								arc.setTarget(port1);
							}
						}
					}
				}

				for (Arc a : glyph_arc_map.get(glyph.getId())) {

					if (a.getSource() instanceof Port) {
						if (((Port) a.getSource()).equals(glyph.getPort().get(Utils.FIRST_PORT))) {
							a.getStart().setX(glyph.getPort().get(Utils.FIRST_PORT).getX());
							a.getStart().setY(glyph.getPort().get(Utils.FIRST_PORT).getY());
						} else if (((Port) a.getSource()).equals(glyph.getPort().get(Utils.SECOND_PORT))) {
							a.getStart().setX(glyph.getPort().get(Utils.SECOND_PORT).getX());
							a.getStart().setY(glyph.getPort().get(Utils.SECOND_PORT).getY());
						}
					}

					if (a.getTarget() instanceof Port) {
						if (((Port) a.getTarget()).equals(glyph.getPort().get(Utils.FIRST_PORT))) {
							a.getEnd().setX(glyph.getPort().get(Utils.FIRST_PORT).getX());
							a.getEnd().setY(glyph.getPort().get(Utils.FIRST_PORT).getY());
						} else if (((Port) a.getTarget()).equals(glyph.getPort().get(Utils.SECOND_PORT))) {
							a.getEnd().setX(glyph.getPort().get(Utils.SECOND_PORT).getX());
							a.getEnd().setY(glyph.getPort().get(Utils.SECOND_PORT).getY());
						}
					}
				}
			}
			correctConnectedArcs(glyph.getGlyph(), listArcs);
		}
	}

	private LINE_POSITION getLinePosition(Glyph glyph, Arc arc) {
		LINE_POSITION eLinePosition = LINE_POSITION.eNeutral;

		float point_x = glyph.getBbox().getX();
		float point_y = glyph.getBbox().getY();

		if (arc.getNext().size() > 0) {
			Next bendPoint = getClosestBendPoint2Coordinates(arc, glyph.getBbox().getX(), glyph.getBbox().getY());
			point_x = bendPoint.getX();
			point_y = bendPoint.getY();
		} else {
			if (arc.getTarget() instanceof Port) {
				point_x = arc.getStart().getX();
				point_y = arc.getStart().getY();
			} else if (arc.getSource() instanceof Port) {
				point_x = arc.getEnd().getX();
				point_y = arc.getEnd().getY();
			}
		}

		float y_shape = (float) (glyph.getBbox().getY() + glyph.getBbox().getH() * 0.5);
		float x_shape = (float) (glyph.getBbox().getX() + glyph.getBbox().getW() * 0.5);

		if (Math.abs(point_y - y_shape) < Math.abs(point_x - x_shape)) {
			eLinePosition = LINE_POSITION.eHorizontal;
		} else if (Math.abs(point_y - y_shape) > Math.abs(point_x - x_shape)) {
			eLinePosition = LINE_POSITION.eVertical;
		}

		return eLinePosition;
	}

	public Next getClosestBendPoint2Coordinates(Arc arc, float x_coord, float y_coord) {
		Next closestBendPoint = arc.getNext().get(0);
		float min_dist = Utils.getPointDistance(closestBendPoint.getX(), closestBendPoint.getY(), x_coord, y_coord);

		for (Next next : arc.getNext()) {
			float dist = Utils.getPointDistance(next.getX(), next.getY(), x_coord, y_coord);
			if (dist < min_dist) {
				min_dist = dist;
				closestBendPoint = next;
			}
		}

		return closestBendPoint;
	}

	public void setArcsToPorts(Map map) {

		// a process is reversible if all connected arcs are of the Production type
		findReversibleProcesses(map.getGlyph(), map);

		// create relationships in the port_arc_map between arcs and the connected ports
		for (Arc arc : map.getArc()) {
			assignPortToArc(arc, map);
		}

		// in case of reversible processes, if all connected nodes were closer to one
		// port and therefore they all were assigned to that port, resulting in having
		// the second port not connected, we need to check this and we will assign the
		// farthest arc from the first port to the second one.
		checkPortsForReversibleProcesses(map.getGlyph(), map);

		// parse the port_arc_map of relationships between arcs and connected ports and
		// create physically (draw) the connection (edge)
		for (Entry<String, PortArcsRelationship> entry : port_arc_map.entrySet()) {
			if (entry.getValue().getPortType() == PortType.SourcePort) {
				for (Arc a : entry.getValue().getConnectedArcs()) {
					a.setSource(entry.getValue().getPort());
				}
			} else if (entry.getValue().getPortType() == PortType.TargetPort) {
				for (Arc a : entry.getValue().getConnectedArcs()) {
					a.setTarget(entry.getValue().getPort());
				}
			}
		}
	}

	private void checkPortsForReversibleProcesses(List<Glyph> list, Map map) {
		for (Glyph glyph : list) {
			if ((Utils.isProcessType(glyph)) && (reversibleSet.contains(glyph.getId()))) {

				Port port1 = glyph.getPort().get(Utils.FIRST_PORT);
				Port port2 = glyph.getPort().get(Utils.SECOND_PORT);

				if (0 != getAllConnectedArcs(port2)) {
					if (0 == getAllConnectedArcs(port1)) {
						Arc farthestArc = getFarthestArc(port2);

						if (!port_arc_map.containsKey(port1.getId())) {
							port_arc_map.put(port1.getId(), new PortArcsRelationship(port1));
						}
						port_arc_map.get(port1.getId()).addArcToSet(farthestArc);
						port_arc_map.get(port2.getId()).removeArcFromSet(farthestArc);
					}
				} else {
					if (0 == getAllConnectedArcs(port2)) {
						Arc farthestArc = getFarthestArc(port1);

						if (!port_arc_map.containsKey(port2.getId())) {
							port_arc_map.put(port2.getId(), new PortArcsRelationship(port2));
						}
						port_arc_map.get(port2.getId()).addArcToSet(farthestArc);
						if (port_arc_map.containsKey(port1.getId())) {
							port_arc_map.get(port1.getId()).removeArcFromSet(farthestArc);
						}
					} else {
						System.out.println("Error: all ports of the processes should have been already populated");
					}
				}
			}

			checkPortsForReversibleProcesses(glyph.getGlyph(), map);
		}

	}

	private Arc getFarthestArc(Port port) {
		Arc farthestArc = null;
		float greatestDist = 0;

		if (port_arc_map.containsKey(port.getId())) {
			for (Arc arc : port_arc_map.get(port.getId()).getConnectedArcs()) {
				float dist = calculateDistance(arc, port);
				if (greatestDist < dist) {
					greatestDist = dist;
					farthestArc = arc;
				}
			}
		}
		return farthestArc;
	}

	private float calculateDistance(Arc arc, Port port) {
		float dist = 0;
		float point_x = 0, point_y = 0;

		if (arc.getNext().size() > 0) {
			Next bendPoint = getClosestBendPoint2Coordinates(arc, port.getX(), port.getY());
			point_x = bendPoint.getX();
			point_y = bendPoint.getY();
		} else {
			if (arc.getSource() instanceof Port) {
				point_x = arc.getEnd().getX();
				point_y = arc.getEnd().getY();
			} else if (arc.getTarget() instanceof Port) {
				{
					point_x = arc.getStart().getX();
					point_y = arc.getStart().getY();
				}
			}
		}

		dist = Utils.getPointDistance(port.getX(), port.getY(), point_x, point_y);
		return dist;
	}

	private int getAllConnectedArcs(Port port) {
		int nConnectedArcs = 0;

		if (port_arc_map.containsKey(port.getId())) {
			nConnectedArcs = port_arc_map.get(port.getId()).getConnectedArcs().size();
		}

		return nConnectedArcs;
	}

	private void findReversibleProcesses(List<Glyph> list, Map map) {
		for (Glyph process : list) {
			if (Utils.isProcessType(process)) {
				boolean bReversible = true;
				for (Arc arc : map.getArc()) {
					if ((isArcConnectedToGlyph(arc, process))
							&& (arc.getClazz().equals(ConverterDefines.SBGN_CONSUMPTION))) {
						bReversible = false;
						break;
					}
				}

				if (bReversible) {
					reversibleSet.add(process.getId());
				}
			}
			findReversibleProcesses(process.getGlyph(), map);
		}

	}

	private void assignPortToArc(Arc arc, Map map) {
		// if the port has not been set yet, i.e. the arc source/ target is an instance
		// of the Glyph class and not of the Port class yet
		Port currentPort = null;
		Port alternativePort = null;
		Glyph glyph = null;

		if (arc.getClazz().equals(ConverterDefines.SBGN_PRODUCTION)) {
			if (arc.getSource() instanceof Glyph) {
				glyph = (Glyph) arc.getSource();

				if (Utils.isProcessType(glyph)) {
					currentPort = calculateCurrentPortOutgoingArc(arc, glyph);
					alternativePort = calculateAlternativePort(currentPort, glyph);
				}
			}
		} else if (arc.getClazz().equals(ConverterDefines.SBGN_CONSUMPTION)) {
			if (arc.getTarget() instanceof Glyph) {

				glyph = (Glyph) arc.getTarget();

				if (Utils.isProcessType(glyph)) {
					currentPort = calculateCurrentPortIncomingArc(arc, glyph);
					alternativePort = calculateAlternativePort(currentPort, glyph);
				}
			}
		}

		else if (arc.getClazz().equals(ConverterDefines.SBGN_LOGIC_ARC)) {
			if (arc.getTarget() instanceof Glyph) {
				glyph = (Glyph) arc.getTarget();

				if (Utils.isOperatorType(glyph)) {
					currentPort = calculateCurrentPortIncomingArc(arc, glyph);
					alternativePort = calculateAlternativePort(currentPort, glyph);
				}
			}
		} else if (arc.getClazz().equals(ConverterDefines.SBGN_CATALYSIS)
				|| arc.getClazz().equals(ConverterDefines.SBGN_STIMULATION)
				|| arc.getClazz().equals(ConverterDefines.SBGN_INHIBITION)
				|| arc.getClazz().equals(ConverterDefines.SBGN_MODULATION)
				|| arc.getClazz().equals(ConverterDefines.SBGN_NECESSARY_STIMULATION)) {
			if (arc.getSource() instanceof Glyph) {

				glyph = (Glyph) arc.getSource();

				if (Utils.isOperatorType(glyph)) {
					currentPort = calculateCurrentPortIncomingArc(arc, glyph);
					alternativePort = calculateAlternativePort(currentPort, glyph);
				}
			}
		}

		if ((currentPort != null) && (alternativePort != null)) {
			if (!port_arc_map.containsKey(currentPort.getId())) {
				port_arc_map.put(currentPort.getId(), new PortArcsRelationship(currentPort));
				port_arc_map.put(alternativePort.getId(), new PortArcsRelationship(alternativePort));
				port_arc_map.get(currentPort.getId()).addArcToSet(arc);

				if (arc.getClazz().equals(ConverterDefines.SBGN_PRODUCTION)) {
					if (!reversibleSet.contains(glyph.getId())) {
						port_arc_map.get(currentPort.getId()).setType(PortType.SourcePort);
						port_arc_map.get(alternativePort.getId()).setType(PortType.TargetPort);
					} else {
						port_arc_map.get(currentPort.getId()).setType(PortType.SourcePort);
						port_arc_map.get(alternativePort.getId()).setType(PortType.SourcePort);
					}
				} else if (arc.getClazz().equals(ConverterDefines.SBGN_CONSUMPTION)) {
					port_arc_map.get(currentPort.getId()).setType(PortType.TargetPort);
					port_arc_map.get(alternativePort.getId()).setType(PortType.SourcePort);
				} else if (arc.getClazz().equals(ConverterDefines.SBGN_LOGIC_ARC)) {
					port_arc_map.get(currentPort.getId()).setType(PortType.TargetPort);
					port_arc_map.get(alternativePort.getId()).setType(PortType.SourcePort);
				} else if (arc.getClazz().equals(ConverterDefines.SBGN_CATALYSIS)
						|| arc.getClazz().equals(ConverterDefines.SBGN_STIMULATION)
						|| arc.getClazz().equals(ConverterDefines.SBGN_INHIBITION)
						|| arc.getClazz().equals(ConverterDefines.SBGN_MODULATION)
						|| arc.getClazz().equals(ConverterDefines.SBGN_NECESSARY_STIMULATION)) {
					port_arc_map.get(currentPort.getId()).setType(PortType.SourcePort);
					port_arc_map.get(alternativePort.getId()).setType(PortType.TargetPort);
				}
			}
			// If the port exists already in the map and it has already some arcs assigned
			// to
			// it, it must check if the existent arcs have the same clazz; if there are arcs
			// of different clazz, they must be assigned to the alternative port. However,
			// if the glyph is a reversible process, the arcs are assinged to the initial
			// calculated port, as in this case, both ports can handle outgoing arcs.
			else {

				if (arc.getClazz().equals(ConverterDefines.SBGN_PRODUCTION)) {
					if (!reversibleSet.contains(glyph.getId())) {
						if (port_arc_map.get(currentPort.getId()).getPortType() == PortType.SourcePort) {
							port_arc_map.get(currentPort.getId()).addArcToSet(arc);
						} else {
							port_arc_map.get(alternativePort.getId()).addArcToSet(arc);
						}
					} else {
						port_arc_map.get(currentPort.getId()).addArcToSet(arc);
					}
				} else if (arc.getClazz().equals(ConverterDefines.SBGN_CONSUMPTION)) {
					if (port_arc_map.get(currentPort.getId()).getPortType() == PortType.TargetPort) {
						port_arc_map.get(currentPort.getId()).addArcToSet(arc);
					} else {
						port_arc_map.get(alternativePort.getId()).addArcToSet(arc);
					}
				} else if (arc.getClazz().equals(ConverterDefines.SBGN_LOGIC_ARC)) {

					if (port_arc_map.get(currentPort.getId()).getPortType() == PortType.TargetPort) {
						port_arc_map.get(currentPort.getId()).addArcToSet(arc);
					} else {
						port_arc_map.get(alternativePort.getId()).addArcToSet(arc);
					}
				} else if (arc.getClazz().equals(ConverterDefines.SBGN_CATALYSIS)
						|| arc.getClazz().equals(ConverterDefines.SBGN_STIMULATION)
						|| arc.getClazz().equals(ConverterDefines.SBGN_INHIBITION)
						|| arc.getClazz().equals(ConverterDefines.SBGN_MODULATION)
						|| arc.getClazz().equals(ConverterDefines.SBGN_NECESSARY_STIMULATION)) {

					if (port_arc_map.get(currentPort.getId()).getPortType() == PortType.SourcePort) {
						port_arc_map.get(currentPort.getId()).addArcToSet(arc);
					} else {
						port_arc_map.get(alternativePort.getId()).addArcToSet(arc);
					}
				}
			}
		}
	}

	private Port calculateAlternativePort(Port currentPort, Glyph glyph) {
		Port alternativePort = null;

		if (currentPort.equals(glyph.getPort().get(Utils.FIRST_PORT))) {
			alternativePort = glyph.getPort().get(Utils.SECOND_PORT);
		} else if (currentPort.equals(glyph.getPort().get(Utils.SECOND_PORT))) {
			alternativePort = glyph.getPort().get(Utils.FIRST_PORT);
		}
		return alternativePort;
	}

	private Port calculateCurrentPortIncomingArc(Arc arc, Glyph glyph) {
		Port currentPort = null;
		Port port1 = glyph.getPort().get(Utils.FIRST_PORT);
		Port port2 = glyph.getPort().get(Utils.SECOND_PORT);

		float point_x = glyph.getBbox().getX();
		float point_y = glyph.getBbox().getY();

		if (arc.getNext().size() > 0) {
			Next bendPoint = getClosestBendPoint2Coordinates(arc, glyph.getBbox().getX(), glyph.getBbox().getY());
			point_x = bendPoint.getX();
			point_y = bendPoint.getY();
		} else {
			point_x = arc.getStart().getX();
			point_y = arc.getStart().getY();
		}

		float dist1 = Utils.getPointDistance(point_x, point_y, port1.getX(), port1.getY());
		float dist2 = Utils.getPointDistance(point_x, point_y, port2.getX(), port2.getY());

		if (dist1 == dist2) {
			currentPort = port1;
		} else if (dist1 < dist2) {
			currentPort = port1;
		} else {
			currentPort = port2;
		}
		return currentPort;
	}

	private Port calculateCurrentPortOutgoingArc(Arc arc, Glyph glyph) {
		Port currentPort = null;
		Port port1 = glyph.getPort().get(Utils.FIRST_PORT);
		Port port2 = glyph.getPort().get(Utils.SECOND_PORT);

		float point_x = glyph.getBbox().getX();
		float point_y = glyph.getBbox().getY();

		if (arc.getNext().size() > 0) {
			Next bendPoint = getClosestBendPoint2Coordinates(arc, glyph.getBbox().getX(), glyph.getBbox().getY());
			point_x = bendPoint.getX();
			point_y = bendPoint.getY();
		} else {
			point_x = arc.getEnd().getX();
			point_y = arc.getEnd().getY();
		}

		float dist1 = Utils.getPointDistance(point_x, point_y, port1.getX(), port1.getY());
		float dist2 = Utils.getPointDistance(point_x, point_y, port2.getX(), port2.getY());

		if (dist1 <= dist2) {
			currentPort = port1;
		} else {
			currentPort = port2;
		}
		return currentPort;
	}

	private boolean isArcConnectedToGlyph(Arc arc, Glyph glyph) {

		boolean bArcAttachedToProcess = false;
		Glyph currentGlyph = null;

		if (arc.getSource() instanceof Glyph) {
			currentGlyph = (Glyph) arc.getSource();
			if (currentGlyph.equals(glyph)) {
				bArcAttachedToProcess = true;
			}
		}

		if (!bArcAttachedToProcess) {
			if (arc.getTarget() instanceof Glyph) {
				currentGlyph = (Glyph) arc.getTarget();
				if (currentGlyph.equals(glyph)) {
					bArcAttachedToProcess = true;
				}
			}
		}

		Port port = null;

		if (!bArcAttachedToProcess) {
			if (arc.getSource() instanceof Port) {
				port = (Port) arc.getSource();
				if (port.equals(glyph.getPort().get(Utils.FIRST_PORT))
						|| (port.equals(glyph.getPort().get(Utils.SECOND_PORT)))) {
					bArcAttachedToProcess = true;
				}
			}
		}

		if (!bArcAttachedToProcess) {
			if (arc.getTarget() instanceof Port) {
				port = (Port) arc.getTarget();
				if (port.equals(glyph.getPort().get(Utils.FIRST_PORT))
						|| (port.equals(glyph.getPort().get(Utils.SECOND_PORT)))) {
					bArcAttachedToProcess = true;
				}
			}
		}

		return bArcAttachedToProcess;
	}
}
