import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { SketchPicker } from 'react-color';
import CheckBox from '../../../components/design/input/CheckBox';
import { getServiceURL } from '../../../utilities/rest';

/**
 * Picking colors for a calendar and switch visibility.
 */
class CalendarStyler extends Component {
    constructor(props) {
        super(props);
        const { background, visible } = this.props;
        this.state = {
            background,
            visible,
        };
        this.handleBackgroundColorChange = this.handleBackgroundColorChange.bind(this);
        this.handleVisibilityChange = this.handleVisibilityChange.bind(this);
    }

    handleBackgroundColorChange(color) {
        this.setState({ background: color.hex });
        const { calendarId } = this.props;
        fetch(getServiceURL('calendar/changeStyle', {
            calendarId,
            bgColor: color.hex,
        }), {
            method: 'GET',
            credentials: 'include',
        })
            .catch(error => alert(`Internal error: ${error}`));
    }

    handleVisibilityChange(event) {
        this.setState({ visible: event.target.checked });
    }

    render() {
        const { background, visible } = this.state;
        // const { translations } = this.props;
        return (
            <React.Fragment>
                <CheckBox
                    // label={translations['calendar.filter.visible']}
                    label="[calendar.filter.visible]"
                    id="opened"
                    onChange={this.handleVisibilityChange}
                    checked={visible}
                />
                <p>
                    Im ColorPicker lassen sich oben
                    <br/>
                    im Farbfeld und im Slider die
                    <br/>
                    Farben nicht auswählen. Dies
                    <br/>
                    müsste an Popper liegen, da das
                    <br/>
                    Colorpicking funktioniert, wenn
                    <br/>
                    der ColorPicker woanders eingebunden
                    <br/>
                    wird.
                </p>
                <SketchPicker
                    color={background}
                    onChangeComplete={this.handleBackgroundColorChange}
                    disableAlpha
                />
                <p>
                    Wie kann ich submitValue überschreiben?
                    <br/>
                    Erst dann soll die Farbe gefetched
                    <br/>
                    werden.
                </p>
            </React.Fragment>
        );
    }
}

CalendarStyler.propTypes = {
    calendarId: PropTypes.number.isRequired,
    background: PropTypes.string,
    visible: PropTypes.bool,
    // translations: PropTypes.shape({}).isRequired,
};

CalendarStyler.defaultProps = {
    background: '#777',
    visible: true,
};

export default (CalendarStyler);
